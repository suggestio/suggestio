package io.suggest.url

package object bind {

  type QsParams = Map[String, Seq[String]]

  /** play.QueryStringBindable.bind() signature.
    * (key, params) => bindingResult.
    */
  type QsBinderF[T] = (String, QsParams) => Option[Either[String, T]]

  /** play.QueryStringBindable.unbind() signature.
    * (key, value) => String
    */
  type QsUnbinderF[T] = (String, T) => String

}
