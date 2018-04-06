package io.suggest.adn.edit.c

import java.net.URI

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.adn.edit.m._
import io.suggest.common.empty.OptionUtil
import io.suggest.err.ErrorConstants
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.{ColorBtnClick, ColorChanged, DocBodyClick}
import io.suggest.model.n2.node.meta.colors.MColorTypes
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.log.Log
import japgolly.univeq._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 19:29
  * Description: Контроллер редактирования узла.
  */
class NodeEditAh[M](
                     modelRW: ModelRW[M, MAdnNodeS]
                   )
  extends ActionHandler(modelRW)
  with Log
{

  private def _filterStrTrimmed(v: String): Boolean = {
    v.nonEmpty && v.trim.nonEmpty
  }

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Изменение цвета.
    case m: ColorChanged =>
      val v0 = value
      v0.colorPicker.fold {
        noChange
      } { cp0 =>
        // Узнать, как цвет сейчас изменён:
        val v2 = v0.withMeta(
          v0.meta.withColors(
            v0.meta.colors
              .withColorOfType(cp0.ofColorType, Some(m.mcd))
          )
        )
        updated(v2)
      }


    // Нажатие кнопки выбора цвета.
    case m: ColorBtnClick =>
      val v0 = value
      val colorType = m.colorTypeOpt.get
      def cp2F = MAdnEditColorPickerS(
        ofColorType = colorType,
        topLeftPx   = m.vpXy.copy(
          x = m.vpXy.x - (if (colorType ==* MColorTypes.Fg) 220 else 0),
          y = m.vpXy.y - 260
        )
      )
      val v2 = v0.colorPicker.fold {
        // Сейчас color picker скрыт. Открыть его:
        v0.withColorPicker( Some(cp2F) )
      } { cp0 =>
        // Какой-то color picker уже открыт. Или закрыть текущий, или открыть другой.
        val cp2Opt = OptionUtil.maybe( cp0.ofColorType !=* colorType )( cp2F )
        val v1 = v0.withColorPicker( cp2Opt )
        // Залить цвет в picker
        v0.meta.colors
          .ofType(colorType)
          .fold(v1) { addColor =>
            v1.withColorPresets {
              val l = v0.colorPresetsLen
              val cps1 = if (l > 10) {
                // Надо укоротить список цветов
                Iterator(
                  v0.colorPresets.view(0, 7),
                  v0.colorPresets.view(l - 2, l)
                )
                  .flatten
                  .toList
              } else {
                v0.colorPresets
              }
              (addColor :: cps1).distinct
            }
          }

      }
      updated(v2)

    case DocBodyClick =>
      val v0 = value
      v0.colorPicker.fold(noChange) { _ =>
        val v2 = v0.withColorPicker( None )
        updated(v2)
      }

    // Выставление новое названия узла:
    case m: SetName =>
      val v0 = value
      val name2 = m.name
      if (name2 ==* v0.meta.name) {
        noChange

      } else {
        // Есть текст, выставить его в состояние. Финальный трим будет на сервере.
        val v1 = v0.withMeta(
          v0.meta.withName(
            m.name
          )
        )

        // Проверить корректность
        val trimmed = m.name.trim
        // TODO Задействовать нормальную валидацию, а не это.
        val v2 = if (trimmed.length > 0) {
          if (v1.errors.name.nonEmpty) {
            v1.withErrors(
              v1.errors.withNameError(None)
            )
          } else {
            v1
          }

        } else {
          // Сохранить в состояние и выставить ошибку name-поля:
          v1.withErrors(
            v1.errors.withNameError(
              Some( MsgCodes.`Error` )    // TODO нужна нормальная ошибка
            )
          )
        }
        updated(v2)
      }

    // Редактирование города
    case m: SetTown =>
      val v0 = value

      val townOpt = Option(m.town)
        .filter(_filterStrTrimmed)

      if (townOpt ==* v0.meta.address.town) {
        noChange

      } else {
        val v1 = v0.withMeta(
          v0.meta.withAddress(
            v0.meta.address.withTown( townOpt )
          )
        )
        updated(v1)
      }


    // Редактирование адреса
    case m: SetAddress =>
      val v0 = value

      val addressOpt = Option(m.address)
        .filter( _filterStrTrimmed )

      if (addressOpt ==* v0.meta.address.address) {
        noChange

      } else {
        val v1 = v0.withMeta(
          v0.meta.withAddress(
            v0.meta.address.withAddress(
              addressOpt
            )
          )
        )
        updated(v1)
      }


    // Редактирование ссылки на сайт.
    case m: SetSiteUrl =>
      val v0 = value

      val urlOpt = Option(m.siteUrl)
        .map(_.trim)
        .filter(_.nonEmpty)

      if (urlOpt ==* v0.meta.business.siteUrl) {
        noChange

      } else {
        val v1 = v0.withMeta(
          v0.meta.withBusiness(
            v0.meta.business.withSiteUrl(
              urlOpt
            )
          )
        )

        // Попытаться распарсить ссылку в тексте.
        val v2 = if (urlOpt.exists { url =>
          Try {
            ErrorConstants.assertArg( url.startsWith( HttpConst.Proto.HTTP ) )
            URI.create(url)
          }.isFailure
        }) {
          v1.withErrors(
            v1.errors.withSiteUrl(
              Some( MsgCodes.`invalid_url` )
            )
          )
        } else {
          if (v1.errors.siteUrl.nonEmpty) {
            v1.withErrors(
              v1.errors.withSiteUrl( None )
            )
          } else {
            v1
          }
        }

        updated(v2)
      }


    // Редактирование инфы о товарах и услугах.
    case m: SetInfo =>
      val v0 = value

      val infoOpt = Option(m.infoAboutProducts)
        .filter( _filterStrTrimmed )

      if (infoOpt ==* v0.meta.business.info) {
        noChange

      } else {
        val v1 = v0.withMeta(
          v0.meta.withBusiness(
            v0.meta.business
              .withInfo( infoOpt )
          )
        )

        updated(v1)
      }


    // Редактирование поля человеческого трафика.
    case m: SetHumanTraffic =>
      val v0 = value

      val htOpt = Option(m.humanTraffic)
        .filter( _filterStrTrimmed )

      if (htOpt ==* v0.meta.business.humanTraffic) {
        noChange

      } else {
        val v1 = v0.withMeta(
          v0.meta.withBusiness(
            v0.meta.business
              .withHumanTraffic( htOpt )
          )
        )

        updated(v1)
      }


    // Редактирование описания аудитории.
    case m: SetAudienceDescr =>
      val v0 = value

      val adOpt = Option(m.audienceDescr)
        .filter( _filterStrTrimmed )

      if (adOpt ==* v0.meta.business.audienceDescr) {
        noChange

      } else {
        val v1 = v0.withMeta(
          v0.meta.withBusiness(
            v0.meta.business
              .withAudienceDescr( adOpt )
          )
        )

        updated(v1)
      }

  }

}
