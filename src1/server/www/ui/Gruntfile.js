// Generated on 2014-12-19 using generator-angular 0.10.0
'use strict';

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'
// 2014.dec.23: Patched from https://github.com/tuplejump/play-yeoman/pull/62

module.exports = function (grunt) {

  // Load grunt tasks automatically
  require('load-grunt-tasks')(grunt);

  // Time how long tasks take. Can help when optimizing build times
  require('time-grunt')(grunt);

  // Configurable paths for the application
  var appConfig = {
    app: require('./bower.json').appPath || 'app',
    dist: 'dist'
  };

  // Define the configuration for all the tasks
  grunt.initConfig({

    // Project settings
    yeoman: appConfig,

    // Watches files for changes and runs tasks based on the changed files
    watch: {
      coffee: {
        files: '<%= yeoman.app %>/scripts/**/*.coffee',
        tasks: ['coffee:compile']
      },
      bower: {
        files: ['bower.json'],
        tasks: ['wiredep']
      },
      js: {
        files: ['<%= yeoman.app %>/scripts/{,*/}*.js'],
        tasks: ['newer:jshint:all'],
        options: {
          livereload: '<%= connect.options.livereload %>'
        }
      },
      styles: {
        files: ['<%= yeoman.app %>/styles/{,*/}*.css'],
        tasks: ['newer:copy:styles', 'autoprefixer']
      },
      gruntfile: {
        files: ['Gruntfile.js']
      },
      livereload: {
        options: {
          livereload: '<%= connect.options.livereload %>'
        },
        files: [
          '<%= yeoman.app %>/{,*/}*.html',
          '.tmp/styles/{,*/}*.css',
          '<%= yeoman.app %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      },
      scalaTpl: {
        files: ['<%= yeoman.app %>/views/**/*.scala.*'],
        tasks: ['newer:copy:twirl-dev']
      }
    },

    // The actual grunt server settings
    connect: {
      options: {
        port: 9000,
        // Change this to '0.0.0.0' to access the server from outside.
        hostname: 'localhost',
        livereload: 35729
      },
      livereload: {
        options: {
          open: true,
          middleware: function (connect) {
            return [
              connect.static('.tmp'),
              connect().use(
                '/bower_components',
                connect.static('./bower_components')
              ),
              connect.static(appConfig.app)
            ];
          }
        }
      },
      test: {
        options: {
          port: 9001,
          middleware: function (connect) {
            return [
              connect.static('.tmp'),
              connect.static('test'),
              connect().use(
                '/bower_components',
                connect.static('./bower_components')
              ),
              connect.static(appConfig.app)
            ];
          }
        }
      },
      dist: {
        options: {
          open: true,
          base: '<%= yeoman.dist %>'
        }
      }
    },

    // Make sure code styles are up to par and there are no obvious mistakes
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      all: {
        src: [
          'Gruntfile.js',
          '<%= yeoman.app %>/scripts/{,*/}*.js'
        ]
      },
      test: {
        options: {
          jshintrc: 'test/.jshintrc'
        },
        src: ['test/spec/{,*/}*.js']
      }
    },

    // Empties folders to start fresh
    clean: {
      // rm dist builds
      dist: {
        files: [{
          dot: true,
          src: [
            '.tmp',
            '<%= yeoman.dist %>/**/*',
	    //'<%= yeoman.dist %>/../twirl/**/*'
            '!<%= yeoman.dist %>/.git{,*/}*'
          ]
        }]
      },
      server: '.tmp'
    },

    // Add vendor prefixed styles
    autoprefixer: {
      options: {
        browsers: ['last 1 version']
      },
      dist: {
        files: [{
          expand: true,
          cwd: '.tmp/styles/',
          src: '{,*/}*.css',
          dest: '.tmp/styles/'
        }]
      }
    },

    // Automatically inject Bower components into the app
    wiredep: {
      app: {
        src: ['<%= yeoman.dist %>/views/**/*.html'],
        ignorePath:  /\.\.\//
      }
    },

    // Renames files for browser caching purposes
    filerev: {
      dist: {
        src: [
          '<%= yeoman.dist %>/scripts/{,*/}*.js',
          '<%= yeoman.dist %>/styles/{,*/}*.css',
          '<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
          '<%= yeoman.dist %>/styles/fonts/*'
        ]
      }
    },

    // Reads HTML for usemin blocks to enable smart builds that automatically
    // concat, minify and revision files. Creates configurations in memory so
    // additional tasks can operate on them
    useminPrepare: {
      html: '<%= yeoman.app %>/index.html',
      options: {
        dest: '<%= yeoman.dist %>',
        flow: {
          html: {
            steps: {
              js: ['concat', 'uglifyjs'],
              css: ['cssmin']
            },
            post: {}
          }
        }
      }
    },

    // Performs rewrites based on filerev and the useminPrepare configuration
    usemin: {
      html: ['<%= yeoman.dist %>/{,*/}*.html'],
      css: ['<%= yeoman.dist %>/styles/{,*/}*.css'],
      options: {
        assetsDirs: ['<%= yeoman.dist %>','<%= yeoman.dist %>/images']
      }
    },

    // The following *-min tasks will produce minified files in the dist folder
    // By default, your `index.html`'s <!-- Usemin block --> will take care of
    // minification. These next options are pre-configured if you do not wish
    // to use the Usemin blocks.
    // cssmin: {
    //   dist: {
    //     files: {
    //       '<%= yeoman.dist %>/styles/main.css': [
    //         '.tmp/styles/{,*/}*.css'
    //       ]
    //     }
    //   }
    // },
    // uglify: {
    //   dist: {
    //     files: {
    //       '<%= yeoman.dist %>/scripts/scripts.js': [
    //         '<%= yeoman.dist %>/scripts/scripts.js'
    //       ]
    //     }
    //   }
    // },
    // concat: {
    //   dist: {}
    // },

    imagemin: {
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/images',
          src: '{,*/}*.{png,jpg,jpeg,gif}',
          dest: '<%= yeoman.dist %>/images'
        }]
      }
    },

    svgmin: {
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/images',
          src: '{,*/}*.svg',
          dest: '<%= yeoman.dist %>/images'
        }]
      }
    },

    // Вместо htmlmin, т.к. тот не работает ни разу. Эта поделка тоже не работает.
    replace: {
      // Базово минифицировать все html и txt шаблоны.
      tplmin: {
	src: ['<%= yeoman.dist %>/**/*.txt'],
	overwrite: true,
	replacements: [
	  {from: /@\*.*?\*@/mg, to: ''},    // strip twirl comments.
	  {from: /^[ \t]+/mg, to: ''}	    // strip spaced BOL offsets
	]
      },
      // Динамические css можно по-сильнее сжать
      csstplmin: {
	src: ['<%= yeoman.dist %>/**/*Css.scala.txt'],
	overwrite: true,
	replacements: [
	  {from: /^\s+/mg, to: ''},	    // BOL offsets, empty lines
	  {from: /}\s+}/mg, to: '}}'}
	]
      },
      // По-жестче минифицировать html-шаблоны.
      htmlmin: {
	src: ['<%= yeoman.dist %>/**/*.html'],  // source files array (supports minimatch)
	overwrite: true,
	replacements: [
	  /*{
	    from: 'Foo',
	    to: function (matchedWord) {   // callback replacement
	      return matchedWord + ' Bar';
	    }
	  },*/
	  //{from: /@\*.*?\*@/mg, to: ''},  /* strip twirl comments. */
	  //{from: /^\s+/mg, to: ''},	    /* strip BOL offsets, empty lines */
	  {from: /^\s+/mg, to: ''},	    // strip BOL offsets
	  {from: /\s\s+/mg, to: ' '},	    /* strip 2+ whitespaces */
	  {from: />[\n\r]+/mg, to: '>'},    /* strip newlines after html tags */
	  {from: /[\n\r]+}/mg, to: '}'},    /* irShowOneTpl fails to compile with this */
	  //{from: /}}[\n\r]*$/, to: '}\n}\n'},
	  //{from: /(^(?!}))[\n\r]+}/mg, to: '}'},
	  //{from: /{[\n\r]+</, to: '{<'},
	  {from: '\s+/?>', to: ''}
	  //{from: /}[\n\r]+/mg, to: '}'}   /* ломает импорты! */
	]
      },
      jstplmin: {
	src: ['<%= yeoman.dist %>/**/*.js'],  // source files array (supports minimatch)
	overwrite: true,
	replacements: [
	  {from: /^\s+/mg, to: ''},	    // strip BOL offsets
	  {from: /\s\s+/mg, to: ' '},	    /* strip 2+ whitespaces */
	  {from: />[\n\r]+/mg, to: '>'}     /* strip newlines after html tags */
	]
      }
    },

    // Replace Google CDN references
    //cdnify: {
    //  dist: {
    //    html: ['<%= yeoman.dist %>/*.html']
    //  }
    //},

    // Copies remaining files to places other tasks can use
    copy: {
      'twirl-dev': {
        files: [{
          expand: true,
          dot: true,
          cwd: '<%= yeoman.app %>',
          dest: '<%= yeoman.dist %>',
          src: ['**/*.scala.*']
        }]
      },
      dist: {
        files: [{
          expand: true,
          dot: true,
          cwd: '<%= yeoman.app %>',
          dest: '<%= yeoman.dist %>',
          src: [
            '*.{ico,png,txt}',
            '*.html',
            'views/**/*.js',
            'views/**/*.*ml',
            'views/**/*.txt',
            'images/**/*.{webp}',
            'fonts/**/*.*'
          ]
        }, {
          expand: true,
          cwd: '.tmp/images',
          dest: '<%= yeoman.dist %>/images',
          src: ['generated/*']
        }]
      },
      styles: {
        expand: true,
        cwd: '<%= yeoman.app %>/styles',
        dest: '.tmp/styles/',
        src: '{,*/}*.css'
      },
      scripts: {
        expand: true,
        cwd: '<%= yeoman.app %>/scripts',
        dest: '<%= yeoman.dist %>/scripts',
        src: '**/*.js'
      }
    },

    // Run some tasks in parallel to speed up the build process
    concurrent: {
      server: [
        'copy:styles'
      ],
      test: [
        'copy:styles'
      ],
      dist: [
        'copy:styles',
        'imagemin',
        'svgmin'
      ]
    },

    coffee: {
      compile: {
        files: [{
          expand: true,
          cwd: "<%= yeoman.app %>/scripts/",
          src: ["**/*.coffee"],
          dest: '<%= yeoman.dist %>/scripts/',
          ext: '.js'
        }]
      }
    }

  });


  grunt.registerTask('serve', 'Compile then start a connect web server', function (target) {
    if (target === 'dist') {
      return grunt.task.run(['build', 'connect:dist:keepalive']);
    }

    grunt.task.run([
      'clean:server',
      'wiredep',
      //'concurrent:server',
      'autoprefixer',
      'watch'
    ]);
  });

  grunt.registerTask('server', 'DEPRECATED TASK. Use the "serve" task instead', function (target) {
    grunt.log.warn('The `server` task has been deprecated. Use `grunt serve` to start a server.');
    grunt.task.run(['serve:' + target]);
  });

  /*
  grunt.registerTask('test', [
    'clean:server',
    'concurrent:test',
    'autoprefixer',
    'connect:test',
    'karma'
  ]);
  */

  grunt.registerTask('build-dev', [
    'coffee:compile',
    'copy:scripts',
    'wiredep',
    'newer:copy:twirl-dev'
  ]);

  grunt.registerTask('build-js', [
   'coffee',
   'copy:scripts'
  ]);

  grunt.registerTask('build-dist', [
    'clean:dist',
    'coffee:compile',
    'copy:scripts',
    'wiredep',
    'concurrent:dist',
    'autoprefixer',
    //'concat',
    'copy:dist',
    'replace:tplmin',
    'replace:csstplmin',
    'replace:htmlmin',
    'replace:jstplmin'
  ]);

  grunt.registerTask('default', [
    'newer:jshint',
    'test',
    'build-dev'
  ]);
};
