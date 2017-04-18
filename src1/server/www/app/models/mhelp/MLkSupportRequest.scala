package models.mhelp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 15:33
  * Description: Класс данных формы запроса помощи через обратную связь в ЛК.
  */

case class MLkSupportRequest(
  name        : Option[String],
  replyEmail  : String,
  msg         : String,
  phoneOpt    : Option[String] = None
)

