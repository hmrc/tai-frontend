/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.benefits

import akka.dispatch.japi
import akka.stream.impl.fusing.MapAsync
import com.google.inject.name.Named
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.BinderOption
import play.api.mvc.{Action, AnyContent, BodyParser, Call, EssentialAction, Result}
import play.filters.cors.CORSConfig
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.domain.{AgentBusinessUtr, AgentCode, AgentUserId, AtedUtr, AwrsUtr, CtUtr, HmrcMtdVat, HmrcObtdsOrg, Nino, Org, PayeAgentReference, PsaId, PspId, SaAgentReference, SaUtr, Uar, Vrn}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.domain.BenefitComponentType
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{JourneyCacheConstants, TaiConstants, UpdateOrRemoveCompanyBenefitDecisionConstants}
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel

import scala.collection.GenSetLike
import scala.compat.java8.JFunction1
import scala.compat.java8.functionConverterImpls.{FromJavaConsumer, FromJavaDoubleConsumer, FromJavaDoubleFunction, FromJavaDoublePredicate, FromJavaDoubleToIntFunction, FromJavaDoubleToLongFunction, FromJavaDoubleUnaryOperator, FromJavaFunction, FromJavaIntConsumer, FromJavaIntFunction, FromJavaIntPredicate, FromJavaIntToDoubleFunction, FromJavaIntToLongFunction, FromJavaIntUnaryOperator, FromJavaLongConsumer, FromJavaLongFunction, FromJavaLongPredicate, FromJavaLongToDoubleFunction, FromJavaLongToIntFunction, FromJavaLongUnaryOperator, FromJavaPredicate, FromJavaToDoubleFunction, FromJavaToIntFunction, FromJavaToLongFunction, FromJavaUnaryOperator}
import scala.concurrent.Future
import scala.concurrent.java8.FuturesConvertersImpl
import scala.reflect.internal.Precedence
import scala.reflect.internal.util.WeakHashSet
import scala.runtime.{AbstractFunction1, AbstractPartialFunction}
import scala.util.MurmurHash
import scala.util.control.NonFatal
import scala.xml.dtd.ElementValidator
import scala.xml.persistent.Index
import scala.xml.transform.BasicTransformer

class CompanyBenefitController @Inject()(
  employmentService: EmploymentService,
  @Named("End Company Benefit") journeyCacheService: JourneyCacheService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  override implicit val templateRenderer: TemplateRenderer,
  override implicit val partialRetriever: FormPartialRetriever)
    extends TaiBaseController with JourneyCacheConstants with UpdateOrRemoveCompanyBenefitDecisionConstants {

  private val logger = Logger(this.getClass)

  def redirectCompanyBenefitSelection(empId: Int, benefitType: BenefitComponentType): Action[AnyContent] =
    (authenticate andThen validatePerson).async { implicit request =>
      val cacheValues = Map(
        EndCompanyBenefit_EmploymentIdKey -> empId.toString,
        EndCompanyBenefit_BenefitTypeKey  -> benefitType.toString)

      journeyCacheService.cache(cacheValues) map { _ =>
        Redirect(controllers.benefits.routes.CompanyBenefitController.decision())
      }

    }

  def decision: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    (for {
      currentCache <- journeyCacheService.currentCache
      employment <- employmentService
                     .employment(Nino(user.getNino), currentCache(EndCompanyBenefit_EmploymentIdKey).toInt)
    } yield {
      employment match {
        case Some(employment) =>
          val referer = currentCache.get(EndCompanyBenefit_RefererKey) match {
            case Some(value) => value
            case None =>
              request.headers.get("Referer").getOrElse(controllers.routes.TaxAccountSummaryController.onPageLoad.url)
          }

          val form = {
            val benefitType = currentCache.get(EndCompanyBenefit_BenefitTypeKey)
            val benefitDecisionKey = getBenefitDecisionKey(benefitType)
            benefitDecisionKey match {
              case Some(bdk) => {
                val decision = currentCache.get(bdk)
                UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(decision)
              }
              case _ => UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(None)
            }
          }

          val viewModel = CompanyBenefitDecisionViewModel(
            currentCache(EndCompanyBenefit_BenefitTypeKey),
            employment.name,
            form
          )

          val cache = Map(
            EndCompanyBenefit_EmploymentNameKey -> employment.name,
            EndCompanyBenefit_BenefitNameKey    -> viewModel.benefitName,
            EndCompanyBenefit_RefererKey        -> referer)

          journeyCacheService.cache(cache).map { _ =>
            Ok(views.html.benefits.updateOrRemoveCompanyBenefitDecision(viewModel))
          }
        case None => throw new RuntimeException("No employment found")
      }
    }).flatMap(identity) recover {
      case NonFatal(e) => internalServerError(e.getMessage)
    }
  }

  def submitDecision: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    UpdateOrRemoveCompanyBenefitDecisionForm.form.bindFromRequest.fold(
      formWithErrors => {
        journeyCacheService.currentCache.map { currentCache =>
          val viewModel = CompanyBenefitDecisionViewModel(
            currentCache(EndCompanyBenefit_BenefitTypeKey),
            currentCache(EndCompanyBenefit_EmploymentNameKey),
            formWithErrors)
          BadRequest(views.html.benefits.updateOrRemoveCompanyBenefitDecision(viewModel))
        }
      },
      success => {
        val decision = success.getOrElse("")
        val journeyStartRedirection = Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
        val benefitType = journeyCacheService.mandatoryJourneyValue(EndCompanyBenefit_BenefitTypeKey)

        benefitType.flatMap[Result] {
          //Cache if successful
          case Right(bt) => {
            //Get the Key
            getBenefitDecisionKey(Some(bt)) match {
              case Some(bdk) => //Good Key
              {
                //Store the value
                journeyCacheService.cache(bdk, decision).map {
                  //Complete Redirect
                  _ => {
                    decision match {
                      case NoIDontGetThisBenefit => {
                        Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.stopDate())
                      }
                      case YesIGetThisBenefit => {
                        Redirect(controllers.routes.ExternalServiceRedirectController
                          .auditInvalidateCacheAndRedirectService(TaiConstants.CompanyBenefitsIform))
                      }
                      case e => {
                        logger.error(s"Bad Option provided in submitDecision form: $e")
                        journeyStartRedirection
                      }
                    }
                  }
                }
              }
              case _ => { //Default Case - no formable key.
                logger.error(s"Unable to form key for $DecisionChoice using $benefitType")
                Future.successful(journeyStartRedirection)
              }
            }
          }
          case Left(_) => //Otherwise we can't and need to redirect to start of the journey
          {
            logger.error(s"Unable to find $EndCompanyBenefit_BenefitTypeKey when submitting decision")
            Future.successful(journeyStartRedirection)
          }
        }
      }
  }

  def getBenefitDecisionKey(benefitType: Option[String]): Option[String] = {
    benefitType.map(x => s"$x $DecisionChoice")
  }
}
