package util.ext

import io.suggest.ahc.upload.MpUploadSupportDflt

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 19:41
  * Description: Трейт для поддержки multipart upload в service helper'ах.
  */
trait ExtServiceHelperMpUpload
  extends MpUploadSupportDflt
  with IExtMpUploadSupport
