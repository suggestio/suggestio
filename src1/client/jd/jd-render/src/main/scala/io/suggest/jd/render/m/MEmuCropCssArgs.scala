package io.suggest.jd.render.m

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.img.crop.MCrop
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.17 15:10
  * Description: Модель данных по эмулируемому кропу изображения.
  */
object MEmuCropCssArgs {

  implicit def univEq: UnivEq[MEmuCropCssArgs] = UnivEq.derive

}


/** Класс-контейнер данных для имитации кропа на некропанной картинке.
  *
  * @param crop Параметры кропа.
  * @param origWh Размеры оригинального изображения.
  * @param outerWh Размеры контейнера, в который надо вписать итоговое изображение.
  */
case class MEmuCropCssArgs(
                            crop    : MCrop,
                            origWh  : MSize2di,
                            outerWh : ISize2di
                          )
