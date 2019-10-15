package io.suggest.jd.render.u

import diode.data.Pot
import io.suggest.grid.GridCalc
import io.suggest.jd.MJdTagId.{blockExpand, selPathRev}
import io.suggest.jd.{MJdConf, MJdDoc, MJdTagId}
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs, MJdDataJs, MJdRrrProps, MJdRuntime, MJdRuntimeData, MQdBlSize}
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.scalaz.ZTreeUtil._
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
  def mkQdBlockLessData(tpls      : Stream[Tree[(MJdTagId, JdTag)]],
                        prevOpt   : Option[MJdRuntime]    = None,
                       ): HashMap[MJdTagId, Pot[MQdBlSize]] = {
    // Какие теги нужны (на основе шаблонов)
    val wantedQdBls = for {
      tpls      <- tpls
      rootTree  <- tpls.subForest
      stripOrQdBlockLess <- {
        if (rootTree.rootLabel._2.name ==* MJdTagNames.DOCUMENT) {
          rootTree.subForest
        } else {
          rootTree #:: Stream.empty
        }
      }
      jdtWithId = stripOrQdBlockLess.rootLabel
      if jdtWithId._2.name ==* MJdTagNames.QD_CONTENT
    }
      yield jdtWithId

    if (wantedQdBls.isEmpty) {
      // Нет никаких данных по qd-blockless вообще.
      HashMap.empty[MJdTagId, Pot[MQdBlSize]]

    } else {
      val potEmpty = Pot.empty[MQdBlSize]
      val wantedQdBlsMap = HashMap(
        wantedQdBls
          .map { case (k,_) => (k, potEmpty) } : _*
      )

      // Есть старая карта данных. Дополнить её, замёржив новые данные, и но удаляя ненужные
      (for {
        prev <- prevOpt
        prevMap0 = prev.data.qdBlockLess
        if prevMap0.nonEmpty
      } yield {
        // Найти и выкинуть удалённые элементы из существующей карты:
        val jdIdsDeleted = prevMap0.keySet -- wantedQdBlsMap.keySet
        val prevMap1 = prevMap0 -- jdIdsDeleted
        // Объединить старую и новую карты. Уже есть такой тег в старой карте.
        prevMap1.merged( wantedQdBlsMap ) { (kv0, _) => kv0 }
      })
        .getOrElse( wantedQdBlsMap )
    }
  }


  /** Создание индексированной версии дерева "изнутри".
    * Ленивая. Сложность O(N).
    * Можно превращать в плоские данные для индекса через Tree().flatten.
    */
  def mkTreeIndexed(jdDoc: MJdDoc): Tree[(MJdTagId, JdTag)] = {
    val rootJdt = jdDoc.template.rootLabel

    jdDoc
      .template
      .zipWithIndex
      .deepMapFold( jdDoc.jdId ) { case (jdId, (jdt, i)) =>
        // Для каждого не-верхнего элемента требуется увеличить selPath:
        var updAcc = List.empty[MJdTagId => MJdTagId]

        if (jdt ne rootJdt)
          updAcc ::= selPathRev.modify(i :: _)

        if (jdt.name ==* MJdTagNames.STRIP) {
          val expandMode2 = jdt.props1.bm
            .flatMap(_.expandMode)
          if (expandMode2 !=* jdId.blockExpand)
            updAcc ::= blockExpand.set(expandMode2)
        }

        // Выставить blockExpand, если требуется:
        val jdId2 =
          if (updAcc.isEmpty) jdId
          else updAcc.reduce(_ andThen _)(jdId)

        val el2 = jdId2 -> jdt
        // Вернуть обновлённый jd-id в качестве акка для возможных дочерних итераций.
        (jdId2, el2)
      }
  }


  object mkRuntime {

    val jdDocs      = GenLens[mkRuntime](_.jdDocs)
    val prevOpt     = GenLens[mkRuntime](_.prevOpt)

    implicit class OpsExt( val args: mkRuntime ) extends AnyVal {

      def docs[D: JdDocsGetter](from: D): mkRuntime =
        jdDocs.set( implicitly[JdDocsGetter[D]].apply(from) )(args)

      def prev[P: JdRuntimeGetter](from: P): mkRuntime =
        prevOpt.set( implicitly[JdRuntimeGetter[P]].apply(from) )(args)

      /** Финальная сборка состояния рантайма. Сравнительно ресурсоёмкая операция. */
      def result: MJdRuntime = {
        val tplsIndexed = args.jdDocs.map( mkTreeIndexed )
        val jdTagsById = MJdTagId.mkTreeIndex( tplsIndexed.flatMap(_.flatten) )

        val tpls = args.jdDocs.map(_.template)

        val jdRtData = MJdRuntimeData(
          jdtWideSzMults  = GridCalc.wideSzMults(tpls, args.jdConf),
          jdTagsById      = jdTagsById,
          qdBlockLess     = mkQdBlockLessData(
            tpls    = tplsIndexed,
            prevOpt = args.prevOpt,
          ),
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
    * @param prevOpt Предыдущее состояние, если было.
    *                Некоторые данные переносятся с предшествующего состояния.
    * @return Инстанс [[MJdRuntime]].
    */
  case class mkRuntime(
                        jdConf        : MJdConf,
                        jdDocs        : Stream[MJdDoc]        = Stream.empty,
                        prevOpt       : Option[MJdRuntime]    = None,
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
