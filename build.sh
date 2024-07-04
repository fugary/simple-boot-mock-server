cd simple-boot-mock-newui
npm install
npm run build
cd ..
rm -rf simple-boot-mock-server/src/main/resources/static/
cp -R simple-boot-mock-newui/dist simple-boot-mock-server/src/main/resources/static
mvn clean install -Dmaven.test.skip=true -Pproduction
