package bsh;

import bsh.ParseEvent;

interface EventReceiver {
  Object receive(Object var1, ParseEvent var2, Object... var3);
}


