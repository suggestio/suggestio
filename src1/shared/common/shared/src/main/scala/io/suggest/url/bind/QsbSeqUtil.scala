package io.suggest.url.bind

import io.suggest.common.qs.QsConstants

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
object QsbSeqUtil {

  implicit def qsbSeqQsB[T](implicit innerB: QsBindable[T]): QsBindable[Seq[T]] = {
    new QsBindable[Seq[T]] {
      override def bindF: QsBinderF[Seq[T]] = {
        (key, params) =>
          // Интересующие нас ключи все начинаются одинаково:
          val keyPrefix = key + QsConstants.QS_KEY_INDEX_PREFIX
          val keySufCh = QsConstants.QS_KEY_INDEX_SUFFIX

          // Определить множество индексированных префиксов ключей с индексами.
          val keysSet = params
            .keysIterator
            // Только ключи, относящиеся к текущей обработке
            .filter( _.startsWith(keyPrefix) )
            // Привести полные qs-ключи вида "x[12].a.b"  к  "x[12]"
            .flatMap { kFull =>
              val closingBrakedIndex = kFull.indexOf(keySufCh, keyPrefix.length)
              if (closingBrakedIndex <= 0) {
                println(s"${QsbSeqUtil.getClass.getSimpleName}.bind($key): invalid qs-key: $kFull, params =\n $params")
                Nil
              } else {
                val k = kFull.substring(0, closingBrakedIndex + 1)
                k :: Nil
              }
            }
            // Отбросить неУникальные результаты.
            .toSet

          if (keysSet.isEmpty) {
            // Нет ничего интересного. Результат работы очевиден. None возвращать не следует.
            Some( Right(Nil) )

          } else {

            // В сорцах play для каждого биндинга создают новый объект в рассчёте на возможный stateful qsb.
            // У нас stateful-binder'ов нет, поэтому кешируем инстанс биндера на весь цикл.
            type Acc_t = Either[List[String], List[T]]
            keysSet
              // Восстановить порядок ключей согласно индексам.
              .toSeq
              .sorted
              .iterator
              // Сразу раскрываем все Option'ы, т.к. они там с ведома нижележащего QSB. Будь ошибка, то вернули бы Some(Left()).
              .flatMap { k =>
                innerB.bindF(k, params)
              }
              // Собрать в кучу все полученные результаты биндов
              .foldLeft [Acc_t] (Right(Nil)) { (acc0, eith) =>
                acc0.fold[Acc_t](
                  // Сбор ошибок в аккамулятор ошибок.
                  { errs =>
                    eith.fold[Acc_t](
                      { e => Left(e :: errs) },
                      { _ => acc0 }
                    )
                  },
                  // Режим сбора положительных результатов биндинга: в обратном порядке.
                  { racc =>
                    eith.fold[Acc_t](
                      { e => Left(e :: Nil) },
                      { r => Right(r :: racc) }
                    )
                  }
                )
              }
              // Полученный аккамулятор привести к результату bind().
              .fold [Option[Either[String, Seq[T]]]] (
                // Ошибки: конкатенировать в прямом порядке.
                {errsRev =>
                  val errMsg = errsRev.reverse.mkString("\n")
                  Some( Left( errMsg ) )
                },
                // Результаты биндинга: вернуть в прямом порядке.
                {res =>
                  // Нельзя возвращать None при пустом списке результатов, т.к. пустой список с ведома нижележащих qsb -- тоже список.
                  Some( Right( res.reverse ))
                }
              )
          }
      }

      override def unbindF: QsUnbinderF[Seq[T]] = { (key, value) =>
        if (value.isEmpty) {
          ""
        } else {
          val kInxPrefix = QsConstants.QS_KEY_INDEX_PREFIX
          val kInxSuf = QsConstants.QS_KEY_INDEX_SUFFIX
          QueryStringBindableUtil._mergeUnbinded {
            value
              .iterator
              .zipWithIndex
              .map { case (v, i) =>
                val k = s"$key$kInxPrefix$i$kInxSuf"
                innerB.unbindF(k, v)
              }
          }
        }
      }
    }
  }

}
