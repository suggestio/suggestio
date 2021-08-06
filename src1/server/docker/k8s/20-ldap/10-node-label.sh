#!/bin/bash 

NODE=$1

kubectl label node "$NODE" "io.suggest.ldap=here"
