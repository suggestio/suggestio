package io.suggest.xplay.qsb

import play.api.mvc.QueryStringBindable

/**
  * Обычно в проекте используется смесь QueryStringBindable with [[QsbKey1T]].
  * Тут -- абстрактный класс, облегчающий жизнь компилятора и снижающий ресурсопотребление
  * скомпиленных реализаций QSB.
  */
abstract class QueryStringBindableImpl[T]
  extends QueryStringBindable[T]
  with QsbKey1T
  with QsbSioJsRevRouter[T]
