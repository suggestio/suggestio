define () ->

  init: () ->
    
    myAppModule = angular.module "myApp", []
    
    # configure the module.
    # in this example we will create a greeting filter
    myAppModule.filter(
      "greet",
      () ->
        return (name) ->
          return "Hello, #{name} !"
    )
