package io.suggest.adv.ext.model.ctx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 12:14
 * Description: Утиль для построения моделей MJsCtx.
 */
trait MJsCtxFieldsT {

  def ACTION_FN   = "a"
  def ADS_FN      = "b"
  def TARGET_FN   = "c"
  def STATUS_FN   = "d"
  def DOMAIN_FN   = "e"
  def SERVICE_FN  = "f"
  def ERROR_FN    = "g"
  def CUSTOM_FN   = "h"

}



object MAdCtx {

  def ID_FN       = "a"
  def PICTURE_FN  = "p"
  def CONTENT_FN  = "c"
  def SC_URL_FN   = "u"

}


object MAdContentCtx {

  def FIELDS_FN       = "fields"
  def TITLE_FN        = "title"
  def DESCR_FN        = "descr"

}


object MAdContentField {

  def TEXT_FN = "text"

}


object MAdPictureCtx {

  def SIZE_FN     = "size"
  def UPLOAD_FN   = "upload"
  def SIO_URL_FN  = "sioUrl"
  def SAVED_FN    = "saved"

}


object Size2dCtx {

  def WIDTH_FN = "width"
  def HEIGHT_FN = "height"

}


object PicUploadCtx {

  def MODE_FN         = "mode"
  def URL_FN          = "url"
  def PART_NAME_FN    = "partName"

  // TODO Запилить отдельный enum
  def MODE_S2S        = "s2s"

}

object MErrorInfo {
  def MSG_FN  = "msg"
  def ARGS_FN = "args"
}


trait MExtTargetT {
  def URL_FN          = "url"
  /** В поле с этим именем хранится адрес, на который надобно перекинуть юзера. */
  def ON_CLICK_URL_FN = "href"
  def NAME_FN         = "name"
}

