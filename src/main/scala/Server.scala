package test

object Server extends App {

  unfiltered.netty.
    Http(9000).
    chunked(5 * 1024 * 1024).
    plan(PictureApi).
    run
}
