package io.suggest.sec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.09.2020 22:07
  * Description: Константы для сессии.
  */
object SessionConst {

  /** id кукиса с токеном сессии.
    * На сервере хранится в application.conf, поэтому если менять, то одновременно оба,
    * а лучше вообще не менять никогда, т.к. установленные приложения обновляются не быстро.
    */
  final def SESSION_COOKIE_NAME = "s"

}
