package io.suggest.mbill2.m.tags

import io.suggest.slick.profile.IProfile

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:59
  * Description: Поддержка опциональной колонки tag_face с названием тега.
  */
trait TagFaceOptSlick extends IProfile {

  import profile.api._

  def TAG_FACE_FN = "tag_face"

  trait TagFaceOptColumn { that: Table[_] =>
    def tagFaceOpt = column[Option[String]](TAG_FACE_FN, O.Length(64))
  }

}
