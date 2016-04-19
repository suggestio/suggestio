package models.msc.map

import io.suggest.model.geo.EnvelopeGs
import io.suggest.model.play.qsb.QsbFixedKeyOuter
import io.suggest.sc.map.ScMapConstants.Mqs
import play.api.mvc.QueryStringBindable
import util.PlayLazyMacroLogsImpl
import views.js.sc.m.scMapAreaInfoJsUnbindTpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 16:16
  * Description: QSB-модель с инфой по отображаемой области карты.
  * В качестве модели ограничивающих точек используется envelope.
  */
object MMapAreaInfo extends PlayLazyMacroLogsImpl with QsbFixedKeyOuter {

  override val _FIXED_KEY = Mqs.AREA_INFO_FN

  /** Поддержка линковки в play router. */
  implicit def qsb(implicit
                   envelopeB: QueryStringBindable[EnvelopeGs],
                   doubleB  : QueryStringBindable[Double]
                  ): QueryStringBindable[MMapAreaInfo] = {

    new QueryStringBindable[MMapAreaInfo] with QsbFixedKey {

      override def KEY_DELIM = Mqs.Full.MAP_DELIM

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MMapAreaInfo]] = {
        _checkQsKey(key)
        for {
          envelopeEith    <- envelopeB.bind ( k(Mqs.ENVELOPE_FN),   params )
          zoomEith        <- doubleB.bind   ( k(Mqs.ZOOM_FN),       params )
        } yield {
          for {
            envelope      <- envelopeEith.right
            zoom          <- zoomEith.right
          } yield {
            MMapAreaInfo(
              envelope    = envelope,
              zoom        = zoom
            )
          }
        }
      }

      override def unbind(key: String, value: MMapAreaInfo): String = {
        _checkQsKey(key)
        Seq(
          envelopeB.unbind( k(Mqs.ENVELOPE_FN),  value.envelope  ),
          doubleB.unbind  ( k(Mqs.ZOOM_FN),      value.zoom      )
        )
          .mkString("&")
      }

      override def javascriptUnbind: String = {
        scMapAreaInfoJsUnbindTpl(KEY_DELIM).body
      }
    }
  }

}


case class MMapAreaInfo(
  envelope      : EnvelopeGs,
  zoom          : Double
)
