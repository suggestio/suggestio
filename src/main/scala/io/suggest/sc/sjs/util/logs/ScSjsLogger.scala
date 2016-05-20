package io.suggest.sc.sjs.util.logs

import io.suggest.sjs.common.util.SjsLogger

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.16 9:29
  * Description: Трейт логгирования, используемый в sc-sjs.
  *
  * Вынесен в отдельный трейт для унифицированного управления логгированием в выдаче.
  * Появился накануне внедрения server-side логгирования через MRemoteError.
  */
trait ScSjsLogger
  extends SjsLogger
