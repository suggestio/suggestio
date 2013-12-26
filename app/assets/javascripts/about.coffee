about_slides =
  current_slide : 'aboutLiveSearch'
  current_slide_index : 1
  init : () ->

    $('#aboutControls span').bind "click", () ->

      slide = $(this).attr "data-slide"
      slide_index = $(this).attr "data-slide-index"

      if slide_index == about_slides.current_slide_index
        return false

      $('#aboutControls span').removeClass "active"
      $(this).addClass "active"

      if slide_index < about_slides.current_slide_index
        in_slide_start_params = {'left' : '-100%'}
        out_slide_end_params = {'left' : '100%'}
      else
        in_slide_start_params = {'left' : '100%'}
        out_slide_end_params = {'left' : '-100%'}

      zero_left_slide_params =
        'left' : '0%'

      ## Установить начальное положение in слайда

      in_slide = $('#' + slide)

      $('.slideable-container').animate {'height' : in_slide.height()}, 150

      in_slide.css in_slide_start_params

      cb = () ->
        ## Out slide
        $('#' + about_slides.current_slide ).animate out_slide_end_params

        ## In slide
        in_slide.animate zero_left_slide_params

        about_slides.current_slide = slide
        about_slides.current_slide_index = slide_index

      setTimeout cb, 5

window[about_slides] = about_slides
about_slides.init()