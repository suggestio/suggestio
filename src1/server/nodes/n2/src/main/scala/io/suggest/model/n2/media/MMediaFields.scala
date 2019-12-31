package io.suggest.model.n2.media

import io.suggest.model.PrefixedFn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 10:26
  * Description: Названия ES-полей модели MMedia.
  */
object MMediaFields {

  /** id узла в [[io.suggest.model.n2.node.MNodes]]. */
  val NODE_ID_FN        = "ni"


  /** Поля [[MFileMeta]] */
  object FileMeta extends PrefixedFn {

    val FILE_META_FN    = "fm"
    override protected def _PARENT_FN = FILE_META_FN

    def FM_MIME_FN               = _fullFn( MFileMeta.Fields.MIME_FN )
    def FM_MIME_AS_TEXT_FN       = _fullFn( MFileMeta.Fields.MIME_FN + "." + MFileMeta.Fields.MIME_AS_TEXT_FN )

    /** Full FN nested-поля с хешами. */
    def FM_HASHES_FN             = _fullFn( MFileMeta.Fields.HASHES_HEX_FN )

    def FM_HASHES_TYPE_FN        = _fullFn( MFileMeta.Fields.HashesHexFields.HASH_TYPE_FN )
    def FM_HASHES_VALUE_FN       = _fullFn( MFileMeta.Fields.HashesHexFields.HASH_VALUE_FN )

    def FM_SIZE_B_FN             = _fullFn( MFileMeta.Fields.SIZE_B_FN )

    def FM_IS_ORIGINAL_FN        = _fullFn( MFileMeta.Fields.IS_ORIGINAL_FN )

  }


  /** Поля [[MPictureMeta]] */
  object PictureMeta {
    val PICTURE_META_FN = "pm"
  }


  /** Поля [[io.suggest.model.n2.media.storage.MStorages]]. */
  object Storage {
    val STORAGE_FN      = "st"
  }

}
