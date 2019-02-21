package io.suggest.adv.ext.model.ctx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 12:14
 * Description: Утиль для построения моделей MJsCtx.
 */
trait MJsCtxFieldsT {

  def ACTION_FN         = "a"
  def ADS_FN            = "b"
  def TARGET_FN         = "c"
  def STATUS_FN         = "d"
  def DOMAIN_FN         = "e"
  def SERVICE_FN        = "f"
  def ERROR_FN          = "g"
  def CUSTOM_FN         = "h"
  /** Название поля, которое содержит все цели сразу. */
  def SVC_TARGETS_FN    = "i"
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



object PicUploadCtx {

  def MODE_FN         = "mode"
  def URL_FN          = "url"
  def PART_NAME_FN    = "partName"

}

object MErrorInfo {
  /** Название поля с кодом сообщения об ошибке. */
  def MSG_FN        = "m"
  /** Название поля с массивом параметров рендера сообщения. */
  def ARGS_FN       = "a"
  /** Название поля с технической информаций по ошибке в виде произвольного JSON. */
  def INFO_FN       = "i"
}


trait MExtTargetT {

  /** id текущей цели на стороне хранилища suggest.io. */
  def ID_FN           = "id"

  /** Ссылка на цель, забитая юзером в форму. */
  def URL_FN          = "url"

  /** В поле с этим именем хранится адрес, на который надобно перекинуть юзера. */
  def ON_CLICK_URL_FN = "href"

  /** Название цели, если задано/определено. Может перезаписываться на стороне js. */
  def NAME_FN         = "name"

  /** Метаданные цели, составляемые и обрабатываемые на стороне js в произвольном формате. */
  def CUSTOM_FN       = "custom"

}


/** Названия полей для kv-обмена в поле mctx.custom. */
object MStorageKvCtx {
  /** Название поля ключа. */
  def KEY_FN    = "k"

  /** Название поля значения. */
  def VALUE_FN  = "v"
}
