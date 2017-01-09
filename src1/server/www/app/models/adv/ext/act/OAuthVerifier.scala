package models.adv.ext.act

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 23:20
 * Description: Модель для передачи данных об OAuth verifier (строка) в актор.
 */
case class OAuthVerifier(verifier: Option[String])
