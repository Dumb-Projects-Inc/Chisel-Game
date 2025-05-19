{
  description = "A Nix-flake-based Scala development environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  nixConfig = {
    extra-substituters = [
      "https://circt1620.cachix.org"
    ];
    extra-trusted-public-keys = [
      "circt1620.cachix.org-1:/1sHl+1qjimZwMDWv+3DV+kKzGqEkPLmctaBQSdV8a0="
    ];
  };

  outputs = {
    self,
    nixpkgs,
  }: let
    javaVersion = 21; # Change this value to update the whole stack

    supportedSystems = ["x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin"];
    forEachSupportedSystem = f:
      nixpkgs.lib.genAttrs supportedSystems (system:
        f {
          pkgs = import nixpkgs {
            inherit system;
            overlays = [
              self.overlays.default
              (final: prev: {
                circt = prev.callPackage ./circt/package.nix {};
              })
            ];
          };
        });
  in {
    overlays.default = final: prev: let
      jdk = prev."jdk${toString javaVersion}";
    in {
      sbt = prev.sbt.override {jre = jdk;};
      scala = prev.scala_3.override {jre = jdk;};
      inherit jdk;
    };

    devShells = forEachSupportedSystem ({pkgs}: {
      default = pkgs.mkShell {
        packages = [pkgs.sbt pkgs.coursier pkgs.gtkwave pkgs.verilator pkgs.jdk];
      };
      circt-from-source = pkgs.mkShell {
        packages = [pkgs.sbt pkgs.coursier pkgs.gtkwave pkgs.verilator pkgs.circt pkgs.jdk];

        CHISEL_FIRTOOL_PATH = "${pkgs.circt}/bin";
      };
    });

    packages = forEachSupportedSystem ({pkgs}: {
      circt = pkgs.callPackage ./circt/package.nix {};
    });
  };
}
