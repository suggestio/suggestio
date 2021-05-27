package io.suggest.es.model

import scalaz.Need

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 18:01
 * Description: Интерфейс для доступа к метаданным документов ES.
 */
final case class EsDocMeta(
                            id: Option[String] = None,
                            versionNeed: Need[EsDocVersion] = Need( EsDocVersion.empty ),
                          ) {
  def version = versionNeed.value
}

object EsDocMeta {

  def apply(id: Option[String], version: EsDocVersion): EsDocMeta =
    apply( id, Need(version) )

  def from[D](doc: D)(implicit ev: IEsDoc[D]): EsDocMeta = {
    apply(
      id = ev.id(doc),
      versionNeed = Need( ev.version(doc) ),
    )
  }

}