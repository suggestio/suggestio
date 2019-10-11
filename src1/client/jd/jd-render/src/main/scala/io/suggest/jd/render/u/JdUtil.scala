package io.suggest.jd.render.u

import diode.data.Pot
import io.suggest.grid.GridCalc
import io.suggest.jd.{MJdConf, MJdDoc, MJdTagId}
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs, MJdDataJs, MJdRrrProps, MJdRuntime, MJdRuntimeData, MQdBlSize}
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import japgolly.univeq._
import monocle.macros.GenLens
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
                        prevOpt: Option[MJdRuntime] = None): HashMap[JdTag, Pot[MQdBlSize]] = {
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
      HashMap.empty[JdTag, Pot[MQdBlSize]]

    } else {
      // Есть старая карта данных. Дополнить её, замёржив новые данные, и но удаляя ненужные
      prevOpt
        .map(_.data.qdBlockLess)
        .filter { m =>
          // Пустая карта не нужна. Считаем что пустой карты просто нет.
          m.nonEmpty
        }
        .fold {
          (HashMap.newBuilder[JdTag, Pot[MQdBlSize]] ++= wantedQdBls.map(_ -> Pot.empty[MQdBlSize]))
            .result()
        } { prevMap0 =>
          // TODO И надо убрать удалённые элементы
          wantedQdBls.foldLeft(prevMap0) { (acc0, jdtWant) =>
            (acc0 get jdtWant).fold {
              // Нет такого ключа - добавить в карту с Pot.empty
              acc0 + (jdtWant -> Pot.empty[MQdBlSize])
            } { _ =>
              // Уже есть такой тег в старой карте.
              acc0
            }
          }
        }
    }
  }


  object mkRuntime {

    val jdDocs = GenLens[mkRuntime](_.jdDocs)
    val prevOpt = GenLens[mkRuntime](_.prevOpt)

    implicit class OpsExt( val args: mkRuntime ) extends AnyVal {

      def docs[D: JdDocsGetter](from: D): mkRuntime =
        jdDocs.set( implicitly[JdDocsGetter[D]].apply(from) )(args)

      def prev[P: JdRuntimeGetter](from: P): mkRuntime =
        prevOpt.set( implicitly[JdRuntimeGetter[P]].apply(from) )(args)

      /** Финальная сборка состояния рантайма. Сравнителньо ресурсоёмкая операция. */
      def make: MJdRuntime = {
        val tpls = args.jdDocs.map(_.template)
        val jdRtData = MJdRuntimeData(
          jdtWideSzMults  = GridCalc.wideSzMults(tpls, args.jdConf),
          jdTagsById      = MJdTagId.mkTreeIndex( MJdTagId.mkTreesIndexSeg(args.jdDocs) ),
          qdBlockLess     = mkQdBlockLessData(tpls, args.prevOpt),
        )

        // Обновить данные по qd blockless (внеблоковому контенту).
        MJdRuntime(
          jdCss   = JdCss( MJdCssArgs(
            conf    = args.jdConf,
            data    = jdRtData
          )),
          data    = jdRtData,
        )
      }

    }
  }
  /** Генерация инстанса [[MJdRuntime]] из исходных данных.
    *
    * @param jdDocs Данные документов (шаблоны, id и тд).
    * @param jdConf Конфиг рендера.
    * @return Инстанс [[MJdRuntime]].
    */
  case class mkRuntime(
                        jdConf  : MJdConf,
                        jdDocs  : Stream[MJdDoc]        = Stream.empty,
                        prevOpt : Option[MJdRuntime]    = None,
                      )


  /** typeclass-извлекалка значения Stream[MJdDoc] из произвольного типа. */
  trait JdDocsGetter[T] {
    def apply(from: T): Stream[MJdDoc]
  }
  object JdDocsGetter {

    implicit object ManyDocs extends JdDocsGetter[Stream[MJdDoc]] {
      override def apply(from: Stream[MJdDoc]) = from
    }
    implicit object OneDoc extends JdDocsGetter[MJdDoc] {
      override def apply(from: MJdDoc) = ManyDocs( from #:: Stream.empty )
    }
    implicit object JdDataJs extends JdDocsGetter[MJdDataJs] {
      override def apply(from: MJdDataJs) = OneDoc( from.doc )
    }
    implicit object JdArgs extends JdDocsGetter[MJdArgs] {
      override def apply(from: MJdArgs) = JdDataJs( from.data )
    }
    implicit object RrrProps extends JdDocsGetter[MJdRrrProps] {
      override def apply(from: MJdRrrProps) = JdArgs( from.jdArgs )
    }

  }


  /** typeclass-извлекалка значения MJdRuntime из произвольного типа. */
  trait JdRuntimeGetter[T] {
    def apply(from: T): Option[MJdRuntime]
  }
  object JdRuntimeGetter {
    implicit object Self extends JdRuntimeGetter[MJdRuntime] {
      override def apply(from: MJdRuntime) = Some(from)
    }
    implicit object SelfOpt extends JdRuntimeGetter[Option[MJdRuntime]] {
      override def apply(from: Option[MJdRuntime]) = from
    }
    implicit object JdArgs extends JdRuntimeGetter[MJdArgs] {
      override def apply(from: MJdArgs) = Self(from.jdRuntime)
    }
    implicit object RrrProps extends JdRuntimeGetter[MJdRrrProps] {
      override def apply(from: MJdRrrProps) = JdArgs( from.jdArgs )
    }
  }

}
