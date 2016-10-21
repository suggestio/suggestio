package io.suggest.model.play.qsb

import io.suggest.util.MacroLogsImplLazy
import play.api.mvc.QueryStringBindable
import io.suggest.common.qs.QsConstants._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.16 22:06
  * Description: Поддержка индексированного доступа к элементам qs наподобии form list mapping:
  * x[1]=1&x[2]=2.
  *
  * Это удобно при объектах, заныканных внутри индексированных qs-ключей:
  * x[1].a=1 & x[1].b=zzz & x[2].a=2 & ...
  */
object QsbSeq extends MacroLogsImplLazy {

  /** Поддержка биндинга в play routes. */
  implicit def qsb[T: QueryStringBindable]: QueryStringBindable[QsbSeq[T]] = {
    new QueryStringBindableImpl[QsbSeq[T]] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, QsbSeq[T]]] = {
        // Интересующие нас ключи все начинаются одинаково:
        val keyPrefix = key + QS_KEY_INDEX_PREFIX

        // Определить множество индексированных префиксов ключей с индексами.
        val keysSet = params
          .keysIterator
          .filter( _.startsWith(keyPrefix) )
          .flatMap { kFull =>
            // Привести полный qs-ключ вида x[12].a.b к x[12]:
            val closingBrakedIndex = kFull.indexOf(QS_KEY_INDEX_SUFFIX, keyPrefix.length)
            if (closingBrakedIndex <= 0) {
              LOGGER.warn(s"bind($key): invalid qs-key: $kFull, params =\n $params")
              Nil
            } else {
              val k = kFull.substring(0, closingBrakedIndex + 1)
              k :: Nil
            }
          }
          .toSet

        if (keysSet.isEmpty) {
          Some(Right(QsbSeq(Nil)))
        } else {
          // В сорцах play для каждого биндинга создают новый объект в рассчёте на возможный stateful qsb.
          // У нас stateful-binder'ов нет, поэтому кешируем инстанс биндера на весь цикл.
          val qsb1 = implicitly[QueryStringBindable[T]]
          type Acc_t = Either[List[String], List[T]]
          keysSet
            .toSeq
            .sorted
            .iterator
            .flatMap { k =>
              qsb1.bind(k, params)
            }
            .foldLeft [Acc_t] (Right(Nil)) {
              // Режим сбора положительных результатов биндинга: в обратном порядке.
              case (acc0 @ Right(racc), eith) =>
                eith.fold[Acc_t](
                  { e => Left(e :: Nil) },
                  { r => Right(r :: racc) }
                )
              // Режим аккамулирования ошибок: собираются только ошибки биндинга, остальное отбрасывается.
              case (acc0 @ Left(errs), eith) =>
                eith.fold[Acc_t](
                  { e => Left(e :: errs) },
                  { _ => acc0 }
                )
            }
            .fold [Option[Either[String, QsbSeq[T]]]] (
              // Ошибки: конкатенировать в прямом порядке.
              {errsRev =>
                val errMsg = errsRev.reverse.mkString("\n")
                Some( Left( errMsg ) )
              },
              // Результаты биндинга: вернуть в прямом порядке.
              {res =>
                Some( Right( QsbSeq(res.reverse) ))
              }
          )
        }
      }

      override def unbind(key: String, value: QsbSeq[T]): String = {
        if (value.items.isEmpty) {
          ""
        } else {
          val qsb1 = implicitly[QueryStringBindable[T]]
          value.items
            .iterator
            .zipWithIndex
            .map { case (v, i) =>
              val k = s"$key$QS_KEY_INDEX_PREFIX$i$QS_KEY_INDEX_SUFFIX"
              qsb1.unbind(k, v)
            }
            .mkString("&")
        }
      }
    }
  }

}

/** Контейнер qs-индексированной последовательности qs-сериализованных элементов. */
case class QsbSeq[T](items: Seq[T])
