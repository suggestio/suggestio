package util.adv.geo.tag

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import io.suggest.ym.model.NodeGeoLevels
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.02.16 15:15
  * Description: Трейт поддержки adv-билдинга для геотеггов.
  */
trait AgtBuilder extends IAdvBuilder {

  import di._
  import mCommonDi._
  import slick.driver.api._

  private def _PRED   = MPredicates.TaggedBy.Agt
  private def _ITYPE  = MItemTypes.GeoTag


  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  /** Спиливание всех тегов, привязанных через биллинг.
    *
    * @param full ignored.
    */
  override def clearAd(full: Boolean): IAdvBuilder = {
    // Вычистить теги из эджей карточки
    val acc2Fut = for {
      acc0 <- super.clearAd(full).accFut
    } yield {
      val mad2 = acc0.mad.copy(
        edges = acc0.mad.edges.copy(
          out = {
            val p = _PRED
            val iter = acc0.mad
              .edges
              .iterator
              // Все теги и геотеги идут через биллинг. Чистка равносильна стиранию всех эджей TaggedBy.
              .filter( _.predicate != p )
            MNodeEdges.edgesToMap1( iter )
          }
        )
      )
      // Сохранить почищенную карточку в возвращаемый акк.
      acc0.copy(
        mad = mad2
      )
    }
    withAcc( acc2Fut )
  }

  // Код закомменчен, т.к. оказался не нужен, но в будущем возможно пригодится.
  /*override def prepareInstallNew(mitems: Iterable[MItem]): Future[IAdvBuilder] = {
    val b0Fut = super.prepareInstallNew(mitems)
    if (!di.AGT_CREATE_TAG_NODES) {
      b0Fut

    } else {
      for {
        b0    <- b0Fut
        acc1  <- b0.accFut
        acc2Fut = {
          // Нужно узнать, какие теги требуются в mitems, сгруппировав как-то.
          lazy val logPrefix = s"prepareInstallNew(${System.currentTimeMillis}):"
          // Бывает, что тег был на момент создания, и в mitem сохранена подсказка с id узла.
          val tnFut0 = for {
            tagNodeOpt0 <- mNodeCache.maybeGetByIdCached( mitem.rcvrIdOpt )
            tagNode0    = tagNodeOpt0.get
            if tagNode0.common.ntype == MNodeTypes.Tag
          } yield {
            LOGGER.trace(s"$logPrefix Found pre-existed tagNode[${tagNode0.idOrNull}], returning...")
            tagNode0
          }

          val tagFace = mitem.tagFaceOpt.get

          // Может быть узел-тег уже существует, но не существовал в момент заказа
          val tnFut1 = tnFut0.recoverWith { case _: NoSuchElementException =>
            val msearch = new MNodeSearchDfltImpl {
              override def tagFaces: Seq[ITagFaceCriteria] = {
                val cr = MTagFaceCriteria(
                  face      = tagFace,
                  isPrefix  = false
                )
                Seq(cr)
              }
            }
            val tnSearchOptFut = MNode.dynSearchOne(msearch)
            LOGGER.trace(s"$logPrefix Trying to find existing tag node by tag-face: $tagFace")
            tnSearchOptFut
              .map(_.get)
          }

          // Если не найдено готового узла-тега, то значит пора его создать...
          tnFut1.recoverWith { case _: NoSuchElementException =>
            // Собрать и схоронить новый узел-тег.
            val tn = MNode(
              common = MNodeCommon(
                ntype       = MNodeTypes.Tag,
                // Неиспользуемый тег должен быть подобран сборщиком мусорных узлов:
                isDependent = true
              ),
              meta = MMeta(
                basic = MBasicMeta()
              ),
              extras = MNodeExtras(
                tag = Some(MTagExtra(
                  faces = MTagFace.faces2map( MTagFace(mitem.tagFaceOpt.get) )
                ))
              )
            )
            for (tnId <- tn.save) yield {
              LOGGER.debug(s"$logPrefix Created new tag node[$tnId] with face: $tagFace")
              tn.copy(id = Some(tnId))
            }
          }
          ???
        }
      } yield {
        withAcc(acc2Fut)
      }
    }
  }
  */

  /** Установка услуги в карточку. */
  override def install(mitem: MItem): IAdvBuilder = {
    if (mitem.iType == _ITYPE) {
      _installGeoTag(mitem)
    } else {
      super.install(mitem)
    }
  }

  /** Установка тега в карточку. */
  private def _installGeoTag(mitem: MItem): IAdvBuilder = {

    val mitemId = mitem.id.get
    lazy val logPrefix = s"_installGeoTag($mitemId ${System.currentTimeMillis}):"
    LOGGER.trace(s"$logPrefix $mitem")

    val tnIdOpt = None //tagNode.id

    // Синхронно собираем db-экшен для грядущей транзакции...
    val dbAction = {
      val dateStart2 = now
      val dateEnd2   = dateStart2.plus( mitem.dtIntervalOpt.get.toPeriod )
      mItems.query
        .filter { _.id === mitemId }
        .map { i =>
          // Логгер вызывается тут, чтобы не писать ничего, пока не началась реальная запись в БД.
          LOGGER.trace(s"$logPrefix Updating item[$mitemId] dates to: $dateStart2 -> $dateEnd2 ")
          (i.status, i.rcvrIdOpt, i.dateStartOpt, i.dateEndOpt, i.dateStatus)
        }
        .update( (MItemStatuses.Online, tnIdOpt, Some(dateStart2), Some(dateEnd2), dateStart2) )
        .filter(_ == 1)
    }

    val acc2Fut = for {
      // Осталось дождаться готовности аккамулятора с предыдущего шага, и можно собирать результат:
      acc0 <- accFut
    } yield {
      val p = _PRED
      val tagFace = mitem.tagFaceOpt.get

      // Добавляемый гео-шейп:
      val egs = MEdgeGeoShape(
        glevel  = NodeGeoLevels.NGL_TOWN_DISTRICT,
        shape   = mitem.geoShape.get
      )

      // Собираем новый эдж или обновляем существующий.
      val edges0 = acc0.mad.edges
      val edges1iter = edges0
        .withPredicateIter(p)
        .find(_.info.tags.contains(tagFace))
        .fold [TraversableOnce[MEdge]] {
          // Добавить новый эдж в общую кучу
          val ne = MEdge(
            predicate = _PRED,
            nodeIdOpt = tnIdOpt,
            info      = MEdgeInfo(
              tags      = Set(tagFace),
              geoShapes = List(egs)
            )
          )
          LOGGER.trace(s"$logPrefix new edge for ad[${acc0.mad.idOrNull}]: $ne")
          edges0.iterator ++ Iterator(ne)

        } { medge0 =>
          // Модифицировать существующий эдж
          LOGGER.trace(s"$logPrefix updating exising edge: $medge0")
          edges0
            .iterator
            .map {
              case e0 if e0 eq medge0 =>
                e0.copy(
                  info = e0.info.copy(
                    tags      = e0.info.tags + tagFace,
                    geoShapes = egs :: e0.info.geoShapes
                  )
                )

              case e1 => e1
            }
        }

      // Залить ресивера в карточку.

      val mad2 = acc0.mad.copy(
        edges = acc0.mad.edges.copy(
          out = MNodeEdges.edgesToMap1(edges1iter)
        )
      )

      // Собрать и вернуть новый аккамулятор
      acc0.copy(
        mad       = mad2,
        dbActions = dbAction :: acc0.dbActions
      )
    }

    withAcc( acc2Fut )
  }


  /** Деинсталляция услуги из карточки. */
  override def uninstall(mitem: MItem, reasonOpt: Option[String]): IAdvBuilder = {
    if (mitem.iType == _ITYPE) {
      _uninstallGeoTag(mitem, reasonOpt)
    } else {
      super.uninstall(mitem)
    }
  }

  /** Логика снятие с размещения в гео-теге. */
  private def _uninstallGeoTag(mitem: MItem, reasonOpt: Option[String]): IAdvBuilder = {
    val mitemId = mitem.id.get

    lazy val logPrefix = s"_uninstallGeoTag(${mitem.id.orNull} ${System.currentTimeMillis}):"
    LOGGER.trace(s"$logPrefix Unistalling $mitem")

    withAccUpdated { acc0 =>

      // 2016.mar.24: Обновление самой карточки удалено отсюда, т.к. при uninstall карточка теперь ребилдится.

      // Собрать изменения для БД
      val dbAction = {
        val _now = now
        mItems.query
          .filter { _.id === mitemId }
          .map { i => (i.status, i.dateEndOpt, i.dateStatus, i.reasonOpt) }
          // TODO Лучше задействовать тут SQL now() function
          .update((MItemStatuses.Finished, Some(_now), _now, reasonOpt))
          .filter(_ == 1)
      }

      // Собрать новый акк.
      acc0.copy(
        dbActions = dbAction :: acc0.dbActions
      )
    }
  }

}
