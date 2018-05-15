package io.suggest.adn.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.adn.edit.m._
import io.suggest.common.empty.OptionUtil
import io.suggest.lk.m.{ColorBtnClick, ColorChanged, DocBodyClick, PurgeUnusedEdges}
import io.suggest.model.n2.node.meta.{MAddress, MBusinessInfo, MMetaPub}
import io.suggest.model.n2.node.meta.colors.MColorTypes
import io.suggest.sjs.common.log.Log
import japgolly.univeq._

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
                ( v0.colorPresets.view(0, 7) ::
                  v0.colorPresets.view(l - 2, l) ::
                  Nil
                )
                  .flatten
              } else {
                v0.colorPresets
              }
              (addColor :: cps1).distinct
            }
          }
      }
      updated(v2)


    // Клик где-то в документе, чтобы скрыть автоматические попапы. Скрыть color-picker'ы.
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
          v0.meta
            .withName( m.name )
        )

        // Проверить корректность
        val trimmed = m.name.trim
        val trimmedVld = MMetaPub.validateName(trimmed)

        val v2 = trimmedVld.fold(
          {errors =>
            // Сохранить в состояние и выставить ошибку name-поля:
            v1.withErrors(
              v1.errors.withName(
                Some( errors.head )
              )
            )
          },
          {_ =>
            if (v1.errors.name.nonEmpty) {
              v1.withErrors(
                v1.errors.withName(None)
              )
            } else {
              v1
            }
          }
        )
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

        val v2 = MAddress.validateTown(townOpt).fold(
          {errors =>
            v1.withErrors(
              v1.errors
                .withTown( Some(errors.head) )
            )
          },
          {_ =>
            if (v1.errors.town.nonEmpty) {
              v1.withErrors(
                v1.errors.withTown(None)
              )
            } else {
              v1
            }
          }
        )

        updated(v2)
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

        val v2 = MAddress.validateAddress(addressOpt).fold(
          {errors =>
            v1.withErrors(
              v1.errors
                .withAddress( Some(errors.head) )
            )
          },
          {_ =>
            if (v1.errors.address.nonEmpty) {
              v1.withErrors( v1.errors.withAddress(None) )
            } else {
              v1
            }
          }
        )

        updated(v2)
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
            v0.meta.business
              .withSiteUrl( urlOpt )
          )
        )

        // Попытаться распарсить ссылку в тексте.
        val v2 = MBusinessInfo.validateSiteUrl( urlOpt ).fold(
          {errors =>
            v1.withErrors(
              v1.errors
                .withSiteUrl( Some( errors.head ) )
            )
          },
          {_ =>
            if (v1.errors.siteUrl.nonEmpty) {
              v1.withErrors(
                v1.errors.withSiteUrl( None )
              )
            } else {
              v1
            }
          }
        )

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


    // Очистка эджей в состоянии от неактуальных элементов.
    case PurgeUnusedEdges =>
      val v0 = value
      // Собрать множество id используемых эджей.
      val edgeUids = v0.resView
        .edgeUids
        .map(_.edgeUid)
        .toSet
      // Профильтровать карту эджей в состоянии.
      val v2 = v0.withEdges(
        v0.edges.filterKeys(edgeUids.contains)
      )
      updatedSilent(v2)

  }

}
