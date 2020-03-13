package io.suggest.adn.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.adn.edit.m._
import io.suggest.lk.m.PurgeUnusedEdges
import io.suggest.n2.node.meta.{MAddress, MBusinessInfo, MMetaPub}
import io.suggest.scalaz.StringValidationNel
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

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Выставление новое названия узла:
    case m: SetName =>
      val v0 = value
      val name2 = Option.when(m.name.nonEmpty)( m.name )

      val lens = MAdnNodeS.meta
        .composeLens( MMetaPub.name )

      if (name2 ==* lens.get(v0)) {
        noChange

      } else {
        // Есть текст, выставить его в состояние. Финальный трим будет на сервере.
        val v1 = (lens set name2)(v0)

        // Проверить корректность
        val trimmed = m.name.trim
        val v2 = _updateErrors(v1, MMetaPub.validateName(trimmed))( _.name )( _.withName(_) )

        updated(v2)
      }


    // Редактирование города
    case m: SetTown =>
      val v0 = value

      val townOpt = Option(m.town)
        .filter(_filterStrTrimmed)

      val lens = MAdnNodeS.meta
        .composeLens( MMetaPub.address)
        .composeLens( MAddress.town )

      if (townOpt ==* lens.get(v0)) {
        noChange

      } else {
        val v1 = (lens set townOpt)( v0 )

        val v2 = _updateErrors(v1, MAddress.validateTown(townOpt))( _.town )( _.withTown(_) )

        updated(v2)
      }


    // Редактирование адреса
    case m: SetAddress =>
      val v0 = value

      val addressOpt = Option(m.address)
        .filter( _filterStrTrimmed )

      val lens = MAdnNodeS.meta
        .composeLens( MMetaPub.address )
        .composeLens( MAddress.address )

      if (addressOpt ==* lens.get(v0)) {
        noChange

      } else {
        val v1 = (lens set addressOpt)(v0)

        val v2 = _updateErrors(v1, MAddress.validateAddress(addressOpt))( _.address )( _.withAddress(_) )

        updated(v2)
      }


    // Редактирование ссылки на сайт.
    case m: SetSiteUrl =>
      val v0 = value

      val urlOpt = Option(m.siteUrl)
        .map(_.trim)
        .filter(_.nonEmpty)

      val lens = MAdnNodeS.meta
        .composeLens( MMetaPub.business )
        .composeLens( MBusinessInfo.siteUrl )

      if (urlOpt ==* lens.get(v0)) {
        noChange

      } else {
        val v1 = (lens set urlOpt)(v0)

        // Попытаться распарсить ссылку в тексте.
        val v2 = _updateErrors(v1, MBusinessInfo.validateSiteUrl( urlOpt ))( _.siteUrl )( _.withSiteUrl(_) )

        updated(v2)
      }


    // Редактирование инфы о товарах и услугах.
    case m: SetInfo =>
      val v0 = value

      val infoOpt = Option(m.infoAboutProducts)
        .filter( _filterStrTrimmed )

      val lens = MAdnNodeS.meta
        .composeLens( MMetaPub.business )
        .composeLens( MBusinessInfo.info )

      if (infoOpt ==* v0.meta.business.info) {
        noChange

      } else {
        val v1 = (lens set infoOpt)(v0)
        val v2 = _updateErrors(v1, MBusinessInfo.validateInfo( infoOpt ))( _.info )( _.withInfo(_) )

        updated(v2)
      }


    // Редактирование поля человеческого трафика.
    case m: SetHumanTraffic =>
      val v0 = value

      val htOpt = Option(m.humanTraffic)
        .filter( _filterStrTrimmed )

      val lens = MAdnNodeS.meta
        .composeLens( MMetaPub.business )
        .composeLens( MBusinessInfo.humanTraffic )

      if (htOpt ==* lens.get(v0)) {
        noChange

      } else {
        val v1 = (lens set htOpt)(v0)

        val v2 = _updateErrors(v1, MBusinessInfo.validateHumanTraffic( htOpt ))( _.humanTraffic )( _.withHumanTraffic(_) )

        updated(v2)
      }


    // Редактирование описания аудитории.
    case m: SetAudienceDescr =>
      val v0 = value

      val audOpt = Option(m.audienceDescr)
        .filter( _filterStrTrimmed )

      val lens = MAdnNodeS.meta
        .composeLens( MMetaPub.business )
        .composeLens( MBusinessInfo.audienceDescr )

      if (audOpt ==* v0.meta.business.audienceDescr) {
        noChange

      } else {
        val v1 = (lens set audOpt)(v0)

        val v2 = _updateErrors(v1, MBusinessInfo.validateAudienceDescr(audOpt))( _.audienceDescr )( _.withAudienceDescr(_) )

        updated(v2)
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
      val v2 = MAdnNodeS.edges.modify { edges0 =>
        edges0
          .view
          .filterKeys(edgeUids.contains)
          .toMap
      }(v0)

      updatedSilent(v2)

  }


  /** Обновление какой-либо ошибки в состоянии ошибок.
    *
    * @param v0 Начальное состояние.
    * @param vld Результат валидации.
    * @param readF Чтение текущего значения из состояния ошибок..
    * @param writeF Запись нового значения
    * @return Обновлённое состояние.
    */
  private def _updateErrors(v0: MAdnNodeS, vld: StringValidationNel[_])
                           (readF: MAdnEditErrors => Option[String])
                           (writeF: (MAdnEditErrors, Option[String]) => MAdnEditErrors): MAdnNodeS = {
    vld.fold(
      // Ошибка валидации
      {errors =>
        // Записать первую ошибку в состояние.
        val e2 = errors.head
        if (readF(v0.errors) contains e2) {
          v0
        } else {
          MAdnNodeS.errors.modify { errors0 =>
            writeF(errors0, Some(e2))
          }(v0)
        }
      },
      // Всё ок, нет ошибки
      {_ =>
        if (readF(v0.errors).nonEmpty) {
          // стереть ошибку из состояния.
          MAdnNodeS.errors.modify { errors0 =>
            writeF(errors0, None)
          }(v0)
        } else {
          v0
        }
      }
    )
  }

}
