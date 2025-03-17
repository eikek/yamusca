{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
    devshell-tools.url = "github:eikek/devshell-tools";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    devshell-tools,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
      ciPkgs = java-version: with pkgs; [
        devshell-tools.packages.${system}."sbt${java-version}"
      ];
      devshellPkgs =
        (ciPkgs "17")
        ++ (with pkgs; [
          jq
          scala-cli
          bloop
          metals
        ]);
    in {
      formatter = pkgs.alejandra;

      devShells = {
        default = pkgs.mkShellNoCC {
          buildInputs = devshellPkgs;
        };
        ci11 = pkgs.mkShellNoCC {
          buildInputs = ciPkgs "11";
          SBT_OPTS = "-Xmx2G -Xss4m";
        };
        ci17 = pkgs.mkShellNoCC {
          buildInputs = ciPkgs "17";
          SBT_OPTS = "-Xmx2G -Xss4m";
        };
        ci21 = pkgs.mkShellNoCC {
          buildInputs = ciPkgs "21";
          SBT_OPTS = "-Xmx2G -Xss4m";
        };
      };
    });
}
