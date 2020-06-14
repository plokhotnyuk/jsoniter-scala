FROM gitpod/workspace-full
USER gitpod
RUN brew install scala coursier/formulas/coursier sbt
RUN bash -cl "set -eux \
    version=0.8.0 \
    coursier fetch \
        org.scalameta:metals_2.12:$version \
        org.scalameta:mtags_2.13.1:$version \
        org.scalameta:mtags_2.12.10:$version \
        org.scalameta:mtags_2.11.12:$version"