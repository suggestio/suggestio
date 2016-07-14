package controllers.sc

import controllers.SioController
import util.cdn.ICdnUtilDi
import util.di.ILogoUtilDi

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:57
 * Description: Всякая базисная утиль для сборки Sc-контроллера.
 */
trait ScController
  extends SioController
  with ICdnUtilDi
  with ILogoUtilDi
