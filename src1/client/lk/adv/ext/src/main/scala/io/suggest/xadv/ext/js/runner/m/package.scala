package io.suggest.xadv.ext.js.runner

import io.suggest.sjs.common.model.wsproto.MAnswerStatuses

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 12:06
 */
package object m {

  type MAnswerStatus    = MAnswerStatuses.T

  type MService         = MServices.T

  type MCommandType     = MCommandTypes.T

  type MAskAction       = MAskActions.T

  type MPicUploadMode   = MPicUploadModes.T
}
