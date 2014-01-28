package io.suggest.ym.cat

import com.github.tototoshi.csv.CSVReader
import java.io.InputStreamReader
import scala.collection.mutable
import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.14 13:21
 * Description: Поддержка категорий из дерева категорий яндекс-маркета.
 * У этого компонента есть несколько задач:
 * - Описывать базовое дерево всех категорий товаров.
 * - Автоматически подбирать собственные категории магазина под дерево категорий маркета, чтобы
 *   все товары и услуги располагались в едниой иерархии suggest.io.
 * - Быстро парсить значение тега market_category (тем же маппером, по возможности).
 */

object YmCategory {
  /** Тип рекурсирвного дерева категорий. */
  type CatTreeMap_t = Map[String, YmCategory]
  private type MutCatTreeMap_t = mutable.HashMap[String, YmMutCategory]

  private case class YmMutCategory(name: String, subcats: MutCatTreeMap_t)

  @tailrec private def ensureCatPath(path: List[String], mapAcc1: MutCatTreeMap_t) {
    if (!path.isEmpty) {
      val h = path.head
      val sub = mapAcc1.getOrElseUpdate(h, new YmMutCategory(h, new mutable.HashMap))
      ensureCatPath(path.tail, sub.subcats)
    }
  }

  private def compileMutCatTree(mct: MutCatTreeMap_t): CatTreeMap_t = {
    mct.map { case (k, v) =>
      val subcatsOpt = if (v.subcats.isEmpty) {
        None
      } else {
        Some(compileMutCatTree(v.subcats))
      }
      k -> YmCategory(k, subcatsOpt)
    }.toMap
  }


  /** Нужно распарсить в память csv-файл с категориями и загрузить его в память в виде immutable-дерева. */
  val catTree: CatTreeMap_t = {
    val is = getClass.getClassLoader.getResourceAsStream("ym/cat/market_category.csv")
    val reader = new InputStreamReader(is, "UTF-8")
    try {
      val csvReader = CSVReader.open(reader)
      val mapAcc: MutCatTreeMap_t = new mutable.HashMap
      csvReader.iterator.map { l =>
        // Итератор возвращает List(Товары для детей, Для школы, Чертежные принадлежности, "", "")
        // Пятый токен всегда пустой, поэтому его нужно игнорировать. И часто бывает, что другие категории пути тоже пустые.
        val catPath = l.toList filter { !_.isEmpty }
        ensureCatPath(catPath, mapAcc)
      }
      // mutable-карта не является результатом. Нужно сконвертить эту карту в immutable-представление и без мусора на концах.
      compileMutCatTree(mapAcc)

    } finally {
      reader.close()
    }
  }
}


import YmCategory._

sealed case class YmCategory(name: String, subcats: Option[CatTreeMap_t])

