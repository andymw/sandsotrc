# Useful aliases for development on SAND/SOTRC
# To load into your bash session, run '. env.sh'

# commenting out. each machine may be set up differently
#export FINDBUGS_HOME='/usr/share/findbugs'

PROJ_ROOT="$(pwd)"

alias doc="$PROJ_ROOT/view-javadoc.sh"

function ant() {
  pushd "$PROJ_ROOT" &>/dev/null
  /usr/bin/ant "$@"
  popd
}
