package io.suggest.model.n2.extra

import io.suggest.model.n2.node.MNode.Fields.Extras.EXTRAS_FN
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 19:31
 * Description: Аддоны для поддержки поля extras в модели [[io.suggest.model.n2.node.MNode]].
 */
object EMNodeExtras {

  /** Нормальный JSON-формат без обратной совместимости. */
  val FORMAT: Format[MNodeExtras] = {
    (__ \ EXTRAS_FN).formatNullable( MNodeExtras.FORMAT )
      .inmap [MNodeExtras] (
        _ getOrElse MNodeExtras.empty,
        {mne => if (mne.nonEmpty) Some(mne) else None }
      )
  }

  /** Десериализация для тегов требует особого подхода, т.к. теги раньше были на первом уровне. */
  val COMPAT_READS: Reads[MNodeExtras] = {
    __.read( MNodeExtras.OLD_FORMAT )
      .filter( _.tag.nonEmpty )
      .orElse( FORMAT )
  }

  /** Поддержка нетривиальной десериализации в этой модели. */
  implicit val COMPAT_FORMAT: Format[MNodeExtras] = {
    Format[MNodeExtras](COMPAT_READS, FORMAT)
  }

}
