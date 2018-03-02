module.exports = function (grunt) {
    // Displays the elapsed execution time of grunt tasks
    require('time-grunt')(grunt);

    // Project configuration.
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        // Builds Sass
        sass: {
          dev: {
            options: {
                sourceMap: true,
                sourceMapContents: true,
                style: 'expanded'
            },
            files: [{
              expand: true,
              cwd: '../sass/',
              src: ['**/*.scss'],
              dest: '<%= dirs.public %>/stylesheets/',
              ext: '.css'
            }]
          },
          prod: {
            options: {
                style: 'compressed'
            },
            files: [{
              expand: true,
              cwd: '../sass/',
              src: ['**/*.scss'],
              dest: '<%= dirs.public %>/stylesheets/',
              ext: '.min.css'
            }]
          }
        },
        cssmin: {
          public: {
            files: [{
              expand: true,
              cwd: '<%= dirs.public %>/stylesheets/',
              src: ['!*.css', '*.min.css'],
              dest: '<%= dirs.public %>/stylesheets/',
              ext: '.min.css'
            }]
          }
        },
        copy: {
          files: {
            expand: true,
            flatten: true,
            src: ['*.js', '!Gruntfile.js'],
            dest: '../../../public/javascript'
          }
        },
        dirs: {
            public: "../../../public",
            js: "javascript",
        },
        watch: {
            compileCSS: {
                files: ['../sass/**/*.scss'],
                tasks: ['sass']
            }
        },
        concurrent: {
            dev: ['sass'],
            prod:['sass:prod']
        },
        clean : {
            options: {
                force: true
            },
            public: ["<%= dirs.public %>"]
        }
    });

    // will read the dependencies/devDependencies/peerDependencies in your package.json
    // and load grunt tasks that match the provided patterns.
    // Loading the different plugins
    require('load-grunt-tasks')(grunt);


    // Default task(s).
    grunt.registerTask('default', ['dev'])
    grunt.registerTask('dev', ['concurrent:dev', 'cssmin:public', 'copy', 'watch']);
    grunt.registerTask('prod', ['concurrent:prod', 'cssmin:public',  'copy']);
};