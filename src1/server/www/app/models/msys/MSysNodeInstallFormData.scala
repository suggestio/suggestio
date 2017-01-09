package models.msys

import play.api.i18n.Lang

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.16 12:37
  * Description: Модель результата биндинга формы инсталляции узла в sys.
  * Эвакуировано из SysNodeInstall.
  */

case class MSysNodeInstallFormData(
  count : Int,
  lang  : Lang
)
