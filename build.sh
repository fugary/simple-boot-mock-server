cd simple-boot-mock-ui
npm install
npm run build
cd ..
rm -rf simple-boot-mock-server/src/main/resources/static/
cp -R simple-boot-mock-ui/dist simple-boot-mock-server/src/main/resources/static
mvn clean install -Dmaven.test.skip=true -Pproduction
