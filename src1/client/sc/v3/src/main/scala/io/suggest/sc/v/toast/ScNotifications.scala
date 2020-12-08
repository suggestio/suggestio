package io.suggest.sc.v.toast

import diode.{Effect, ModelRO}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MOsFamilies
import io.suggest.geo.DistanceUtil
import io.suggest.i18n.MsgCodes
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.MNodeTypes
import io.suggest.os.notify.{MOsToast, MOsToastText, ShowNotify}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.grid.MScAdData
import io.suggest.text.StringUtil
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.daemon.MDaemonNotifyOpts
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.04.2020 9:07
  * Description: Утиль, рендерящая уведомления для различных целей.
  */
final class ScNotifications(
                             rootRO       : ModelRO[MScRoot],
                           ) {

  private def NOTIFY_AD_TITLE_MAX_LEN = 40

  /** Рендер уведомления о найденных карточках
    *
    * @param unNotifiedAds Карточки, по которым требуются уведомление.
    *                      Желательно, ленивую коллекцию, т.к. проходиться полностью она будет только внутри эффекта.
    * @return Опциональный эффект.
    */
  def adsNearbyFound(unNotifiedAds: Seq[MScAdData]): Option[Effect] = {
    Option.when( unNotifiedAds.nonEmpty ) {
      Effect.action {
        val mroot = rootRO.value
        val messages = mroot.internals.info.commonReactCtx.messages

        // Надо собрать title'ы карточек, добавив расстояния до маячков.
        val adTitlesWithDistRendered = (for {
          scAd <- unNotifiedAds.iterator
          // Используем в работе только карточки с заголовком, присланным с сервера.
          adTitle <- scAd.main.title.iterator
        } yield {
          val distancesCm = (for {
            // Поискать id маячка, с помощью которого найдена карточка.
            adMatchInfo <- scAd.main.info.matchInfos.iterator
            if adMatchInfo.predicates.exists(_ eqOrHasParent MPredicates.Receiver)
            adMatchNodeInfo <- adMatchInfo.nodeMatchings.iterator
            if adMatchNodeInfo.ntype contains MNodeTypes.BleBeacon
            bleBeaconNodeId <- adMatchNodeInfo.nodeId.iterator
            // Есть id маячка. Найти в текущей инфе указанный маячок.
            uidBeacon <- mroot.dev.beaconer
              .nearbyReportById
              .get( bleBeaconNodeId )
              .iterator
            distanceCm <- uidBeacon.distanceCm
          } yield {
            distanceCm
          })
            .to( LazyList )

          Option.when( distancesCm.nonEmpty ) {
            val distanceCm2 = if (distancesCm.lengthIs == 1) {
              distancesCm.head
            } else {
              // Найти среднее арифметическое всех известных расстояний среди имеющихся (одна и та же карточка может быть размещена в нескольких маячках):
              distancesCm.sum / distancesCm.length
            }

            // Переводим расстояние в метры, рендерим в строку:
            val distanceMeters2 = distanceCm2.toDouble / 100
            val distMsg = DistanceUtil.formatDistanceM( distanceMeters2 )

            // Рендерим в строку всё это
            val distanceStr = messages( distMsg )

            // Укоротить заголовок с сервера. 3 - оцениваем как доп."расходы" символов на рендер: скобки, пробелы и т.д. для MsgCodes.`0._inDistance.1`
            val adTitleLenMax = NOTIFY_AD_TITLE_MAX_LEN - distanceStr.length - 3
            val adTitleEllipsied = StringUtil.strLimitLen( adTitle, adTitleLenMax )
            messages( MsgCodes.`0._inDistance.1`, adTitleEllipsied, distanceStr )
          }
            .getOrElse {
              StringUtil.strLimitLen( adTitle, NOTIFY_AD_TITLE_MAX_LEN )
            }
        })
          .to( List )

        val unNotifiedAdsCount = unNotifiedAds.length

        val toast = MOsToast(
          uid = getClass.getSimpleName + `.` + MNodeTypes.BleBeacon.value,
          // Заголовок прост: вывести, что найдено сколько-то предложений рядом
          title = {
            // TODO Переехать на Mozilla Fluent, чтобы отрабатывать детали локализации на уровне каждого конкретного языка.
            if (unNotifiedAdsCount ==* 1) {
              messages( MsgCodes.`One.offer.nearby` )
            } else {
              messages( MsgCodes.`0.offers.nearby`, unNotifiedAdsCount )
            }
          },
          text = MOsToastText(
            text = Option
              .when( adTitlesWithDistRendered.nonEmpty ) {
                adTitlesWithDistRendered
                  .mkString("\n")
              }
              .getOrElse {
                messages(
                  MsgCodes.`Show.offers.0`,
                  mroot.dev.beaconer.nearbyReport
                    .iterator
                    .flatMap(_.distanceCm)
                    .maxOption
                    .fold("") { maxDistanceCm =>
                      // Нет текстов - вывести что-то типа "Показать предложения в радиусе 50 метров".
                      messages(
                        MsgCodes.`in.radius.of.0`,
                        messages( DistanceUtil.formatDistanceCM( maxDistanceCm ) ),
                      )
                    },
                )
              }
          ) :: Nil,
          // Android: нужно задавать smallIcon, т.к. штатная иконка после обесцвечивания в пиктограмму плохо выглядит.
          smallIconUrl = Option.when {
            val p = mroot.dev.platform
            p.isCordova && (p.osFamily contains MOsFamilies.Android)
          }( "res://ic_notification" ),
          // TODO appBadgeCounter - почему-то не работает
          appBadgeCounter = Option.when( unNotifiedAdsCount > 0 )(unNotifiedAdsCount),
          // TODO vibrate - выключить вибрацию? false или None - не помогают.
          vibrate = OptionUtil.SomeBool.someFalse,
          // silent: на android скрывает уведомление вообще
          //silent = OptionUtil.SomeBool.someTrue,
          // foreground: iOS не отображат уведомление на раскрытом приложении. https://github.com/katzer/cordova-plugin-local-notifications/issues/1711
          foreground = OptionUtil.SomeBool.someTrue,
          sticky = OptionUtil.SomeBool.someFalse,
          // Канал: если его не задать в cordova-android, то это уведомление пойдёт внутрь background-геолокации и станет нескрываемым.
          channel = Some( MsgCodes.`Suggest.io` ),
          // TODO icon - вывести круглую иконку узла, в котором может быть находится пользователь? Взять присланную сервером или текущую какую-нибудь?
          // image - без картинки, т.к. это довольно узконаправленное решение.
          /*imageUrl = for {
              mainBlk <- scAd.main.doc.template.loc
                          .findByType( MJdTagNames.STRIP )
              bgImgEdgeId <- mainBlk.getLabel.props1.bgImg
              bgImg <- scAd.main.edges.get( bgImgEdgeId.edgeUid )
              bgImgUrl <- bgImg.jdEdge.url
            } yield {
              HttpClient.mkAbsUrl( bgImgUrl )
            },*/
        )

        ShowNotify( toast :: Nil )
      }
    }
  }


  /** Рендер параметров нотификации по демону.
    *
    * @return Опции нотификации.
    */
  def daemonNotifyOpts(): MDaemonNotifyOpts = {
    val mroot = rootRO.value
    val messages = mroot.internals.info.commonReactCtx.messages

    MDaemonNotifyOpts(
      channelTitle        = Some( messages( MsgCodes.`Background.mode` ) ),
      channelDescr        = Some( messages( MsgCodes.`Background.monitoring.offers.nearby` ) ),
      title               = Some( messages( MsgCodes.`Daemon.toast.title` ) ),
      text                = Some( messages( MsgCodes.`Daemon.toast.descr` ) ),
      resumeAppOnClick    = OptionUtil.SomeBool.someTrue,
    )
  }

}
