let
  pkgs = import <nixpkgs> { };
  initScript = pkgs.writeScript "yamusca-build-init" ''
     export LD_LIBRARY_PATH=
     ${pkgs.bash}/bin/bash -c sbt
  '';
in with pkgs;

buildFHSUserEnv {
  name = "yamusca-sbt";
  targetPkgs = pkgs: with pkgs; [
    nodejs
  ];
  runScript = initScript;
}
