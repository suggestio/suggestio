package models.msc

import play.twirl.api.JavaScript

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 22:12
  * Description: Модель параметров рендера скрипта для React-sc (третье поколение).
  */
case class MSc3ScriptRenderArgs(
                                 state0         : String,
                                 jsMessagesJs   : JavaScript
                               )
