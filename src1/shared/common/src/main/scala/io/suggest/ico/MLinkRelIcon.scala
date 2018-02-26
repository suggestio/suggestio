package io.suggest.ico

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 21:17
  * Description: Класс модели-контейнера с данными для link-rel иконки.
  *
  * @param icon Данные по картинке.
  * @param rels Значения аттрибута rel.
  * @param ieOnly Оборачивание в !--[if IE]--
  */
case class MLinkRelIcon(
                         icon      : MIconInfo,
                         rels      : Seq[String],
                         ieOnly    : Boolean      = false
                       )
