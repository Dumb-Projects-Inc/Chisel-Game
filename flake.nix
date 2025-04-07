{
  description = "A Nix-flake-based Scala development environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

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
            overlays = [self.overlays.default];
          };
        });
  in {
    overlays.default = final: prev: let
      jdk = prev."jdk${toString javaVersion}";
    in {
      sbt = prev.sbt.override {jre = jdk;};
      scala = prev.scala_3.override {jre = jdk;};
    };

    devShells = forEachSupportedSystem ({pkgs}: {
      default = pkgs.mkShell {
        packages = with pkgs; [sbt coursier gtkwave verilator python3 python3Packages.pillow python3Packages.scipy];
      };
    });
  };
}
