{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
      jdk = pkgs.temurin-bin;
      clojure = pkgs.clojure.override {inherit jdk;};
      clojure-lsp = pkgs.clojure-lsp.override {inherit clojure;};
    in {devShell = pkgs.mkShell {buildInputs = with pkgs; [alejandra jdk clojure clojure-lsp];};});
}
