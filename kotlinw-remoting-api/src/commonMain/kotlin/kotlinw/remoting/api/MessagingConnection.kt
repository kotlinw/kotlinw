package kotlinw.remoting.api

interface MessagingConnection<R : Any?, S : Any?> : MessageReceiver<R>, MessageSender<S>
