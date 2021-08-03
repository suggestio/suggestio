#!/bin/bash 

NODE=TODO.suggest.io
echo $NODE

kubectl label node "$NODE" "io.suggest.matrix=here"
