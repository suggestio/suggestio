package models.mup

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 12:00
  * Description: URL qs-модель параметров запуска детектора цветов.
  */
object MColorDetectArgs {

  object Fields {
    val PALETTE_SIZE_FN     = "p"
    val WS_PALETTE_SIZE_FN  = "w"
  }


  implicit def mColorDetectArgsQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MColorDetectArgs] = {
    new QueryStringBindableImpl[MColorDetectArgs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MColorDetectArgs]] = {
        val k = key1F(key)
        val F = Fields
        for {
          paletteSizeE           <- intB.bind( k(F.PALETTE_SIZE_FN),      params )
          wsPaletteSizeE         <- intB.bind( k(F.WS_PALETTE_SIZE_FN),   params )
        } yield {
          for {
            paletteSize          <- paletteSizeE.right
            wsPaletteSize        <- wsPaletteSizeE.right
          } yield {
            MColorDetectArgs(
              paletteSize     = paletteSize,
              wsPaletteSize   = wsPaletteSize
            )
          }
        }
      }

      override def unbind(key: String, value: MColorDetectArgs): String = {
        val F = Fields
        val k = key1F(key)
        _mergeUnbinded1(
          intB.unbind( k(F.PALETTE_SIZE_FN),      value.paletteSize ),
          intB.unbind( k(F.WS_PALETTE_SIZE_FN),   value.wsPaletteSize )
        )
      }

    }
  }

}


/** Класс-контейнер аргументов запуска детектора цветов.
  *
  * @param paletteSize Основной размер палитры.
  * @param wsPaletteSize Размер палитры, присылаемой на клиента через websocket.
  */
case class MColorDetectArgs(
                             paletteSize    : Int,
                             wsPaletteSize  : Int
                           )
