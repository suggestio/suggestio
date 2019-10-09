package io.suggest.jd.render.u

import diode.data.Pot
import io.suggest.common.geom.d2.MSize2di
import io.suggest.grid.GridCalc
import io.suggest.jd.{MJdConf, MJdDoc, MJdTagId}
import io.suggest.jd.render.m.{MJdCssArgs, MJdRuntime}
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import japgolly.univeq._
import scalaz.Tree

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.09.2019 22:33
  * Description: Разная утиль для JD-рендера.
  */
object JdUtil {

  /** Сборка карты состояний QdBlockLess.
    *
    * @param tpls Шаблоны.
    * @param prevOpt Предыдущее состояние.
    * @return Карта состояний.
    */
  def mkQdBlockLessData(tpls: Stream[Tree[JdTag]],
                        prevOpt: Option[MJdRuntime] = None): HashMap[JdTag, Pot[MSize2di]] = {
    // Какие теги нужны (на основе шаблонов)
    val wantedQdBls = for {
      tpls      <- tpls
      rootTree  <- tpls.subForest
      stripOrQdBlockLess <- {
        if (rootTree.rootLabel.name ==* MJdTagNames.DOCUMENT) {
          rootTree.subForest
        } else {
          rootTree #:: Stream.empty
        }
      }
      jdt = stripOrQdBlockLess.rootLabel
      if jdt.name ==* MJdTagNames.QD_CONTENT
    }
      yield jdt

    if (wantedQdBls.isEmpty) {
      // Нет никаких данных по qd-blockless вообще.
      HashMap.empty[JdTag, Pot[MSize2di]]
    } else {
      // Есть старая карта данных. Дополнить её, замёржив новые данные, и но удаляя ненужные
      prevOpt
        .map(_.qdBlockLess)
        .filter { m =>
          // Пустая карта не нужна. Считаем что пустой карты просто нет.
          m.nonEmpty
        }
        .fold {
          (HashMap.newBuilder[JdTag, Pot[MSize2di]] ++= wantedQdBls.map(_ -> Pot.empty[MSize2di]))
            .result()
        } { prevMap0 =>
          // TODO И надо убрать удалённые элементы
          wantedQdBls.foldLeft(prevMap0) { (acc0, jdtWant) =>
            (acc0 get jdtWant).fold {
              // Нет такого ключа - добавить в карту с Pot.empty
              acc0 + (jdtWant -> Pot.empty[MSize2di])
            } { _ =>
              // Уже есть такой тег в старой карте.
              acc0
            }
          }
        }
    }
  }


  /** Генерация инстанса [[MJdRuntime]] из исходных данных.
    * Ресурсоёмкая операция, поэтому лучше вызывать только при сильной необходимости.
    *
    * @param docs Данные документов (шаблоны, id и тд).
    * @param jdConf Конфиг рендера.
    * @return Инстанс [[MJdRuntime]].
    */
  def mkRuntime(
                 docs      : Stream[MJdDoc],
                 jdConf    : MJdConf,
                 // TODO prev - не внедрён полностью, используется в qdBl. И надо сделать increment-режим, чтобы вбрасывать пачку поверх исходного инстанса.
                 prev      : MJdRuntime        = null,
               ): MJdRuntime = {
    val prevOpt = Option(prev)

    val tpls = docs.map(_.template)
    val jdtWideSzMults = GridCalc.wideSzMults(tpls, jdConf)
    val jdTagsById = MJdTagId.mkTreeIndex( MJdTagId.mkTreesIndexSeg(docs) )

    // Обновить данные по qd blockless (внеблоковому контенту).
    MJdRuntime(
      jdCss = JdCss( MJdCssArgs(
        conf            = jdConf,
        jdtWideSzMults  = jdtWideSzMults,
        jdTagsById      = jdTagsById,
      )),
      jdtWideSzMults = jdtWideSzMults,
      // TODO Opt можно оптимизировать сборку id-индекса в выдаче через HashMap.merged для добавления новых данных вместо полного рендера.
      jdTagsById     = jdTagsById,
      qdBlockLess    = mkQdBlockLessData(tpls, prevOpt),
    )
  }

}
