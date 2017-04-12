package com.vivareal.search.api.controller.v2;

import com.vivareal.search.api.controller.v2.stream.ResponseStream;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/v2/listings")
public class ListingController {

    @RequestMapping("/")
    public SearchApiResponse getListings(SearchApiRequest request) {
        return new SearchApiResponse("foi memo!");
    }

    @RequestMapping("/stream")
    public void stream(HttpServletResponse response) throws IOException {
        List<SearchApiResponse> list = IntStream.rangeClosed(1, 10)
                .boxed()
                .map(i -> new SearchApiResponse(String.format(listing, i)))
                .collect(Collectors.toList());

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ResponseStream.create(response.getOutputStream())
                .withIterable(new SearchResponseList(list), obj -> obj.getListings().getBytes());
    }

    private static final String listing = "{\"price\":\"R$ 1.800.000\",\"priceValue\":1800000,\"propertyId\":\"%s\",\"siteUrl\":\"http://www.vivareal.com.br/imovel/cobertura-5-quartos-aldeota-bairros-fortaleza-com-garagem-458m2-venda-RS1800000-id-75055432/\",\"area\":458,\"areaUnit\":null,\"countryName\":\"Brasil\",\"neighborhoodName\":\"Aldeota\",\"cityName\":\"Fortaleza\",\"stateName\":\"CE\",\"zoneName\":\"Bairros\",\"currencySymbol\":\"R$\",\"bathrooms\":5,\"rooms\":5,\"garages\":3,\"latitude\":-3.741135,\"longitude\":-38.497697,\"thumbnails\":[\"http://resizedimgs.vivareal.com/pMvH8vzNb9wXq1IQW48SloLKWU0=/fit-in/221x147/vr.images.sp/48c997b6baf8916708a295f65ce1f5f0.jpg\",\"http://resizedimgs.vivareal.com/BT-cJWO3Q6XChIURgAiSTunY_9E=/fit-in/221x147/vr.images.sp/5e840890e61045683b8d420a63586067.jpg\",\"http://resizedimgs.vivareal.com/4R4XU80tAmzWbRTx_65NfPKj2Ng=/fit-in/221x147/vr.images.sp/beae073b1e938375cbd9896b089db95f.jpg\",\"http://resizedimgs.vivareal.com/uDOAiCH5J_gDeHFVXN_Au6_jkgw=/fit-in/221x147/vr.images.sp/ceb0f906d1dd04bc1302b29cf614ac1a.jpg\",\"http://resizedimgs.vivareal.com/Ht1aeOk6M8T1q2GfxhLEwtuxs0Q=/fit-in/221x147/vr.images.sp/d9212a31f3efe3edd186f7678f030224.jpg\",\"http://resizedimgs.vivareal.com/2gbT6I1-j4BVpJYiZxxf-Vxa5hs=/fit-in/221x147/vr.images.sp/88a894a5e68500be77d99e18893e94fb.jpg\",\"http://resizedimgs.vivareal.com/47NzO7fufrlS4eHZKrZg35ZBdkY=/fit-in/221x147/vr.images.sp/8d1e9da7dbdecbd374b3fc140bea6d8d.jpg\",\"http://resizedimgs.vivareal.com/S8-VX9UwLDaUCNgW_9ktGA5H8v0=/fit-in/221x147/vr.images.sp/46a221927b905826a5ff4ce9a472406b.jpg\",\"http://resizedimgs.vivareal.com/-TfnGm7JLGvm2oeTaf82BU3qmNU=/fit-in/221x147/vr.images.sp/34d7e721d5f65b7fcd861534b8973f63.jpg\",\"http://resizedimgs.vivareal.com/Qb227QMg5eA82i2e26GfC1ZtcOI=/fit-in/221x147/vr.images.sp/aba6fefbde538194140b115ba7c7d26b.jpg\",\"http://resizedimgs.vivareal.com/LgRfREWjryLqrmlHXFoqsDEUqVo=/fit-in/221x147/vr.images.sp/6500019f782d52f3e7d31b3b3c39374b.jpg\",\"http://resizedimgs.vivareal.com/O2f_Cx7J_4vX9nUUTnRnnfJtytc=/fit-in/221x147/vr.images.sp/a126f37182ce44d3ddf32b9b7182f91a.jpg\",\"http://resizedimgs.vivareal.com/WcKKYT4HwISrXWdkJIR_0BKhfxY=/fit-in/221x147/vr.images.sp/cdc48dac1be03b1972561a30e7677cd1.jpg\",\"http://resizedimgs.vivareal.com/B6lSUJgk5ZPlfxOyxIwVkn6MTqo=/fit-in/221x147/vr.images.sp/43f5be159fdbbabdb6ccc6dcf7955f12.jpg\",\"http://resizedimgs.vivareal.com/t1R-oW9Qk6n_GaOzRaLfXS3pD_c=/fit-in/221x147/vr.images.sp/ac5725f9df6a7a992d13e05fbb734c65.jpg\",\"http://resizedimgs.vivareal.com/7fuk4hr6dlCfmQ53vm2nW-RDmfE=/fit-in/221x147/vr.images.sp/f74616f924d67203ee7e4c1519c1be59.jpg\"],\"isDevelopmentUnit\":false,\"saved\":null,\"listingType\":\"PROPERTY\",\"stateNormalized\":\"Ceará\",\"externalId\":\"CO0001\",\"propertyTypeName\":\"Cobertura\",\"propertyTypeId\":\"COBERTURA\",\"title\":\"Cobertura na Aldeota Próximo à Av. Padre Antonio Tomaz\",\"legend\":\"Cobertura Projetada com 458m², Sala 2 Ambientes Ampla, 4 Suites com Closet, Wc Social, Lavabo, Gabinete, Cozinha Projetada, Área de Serviço, Dependência de Empregada Completa, 3 Vagas de Garagem. Condomínio com Salão de Festas, Portaria 24hs, Circuito de TV. Valor do Condomínio - R$ 1.570,00.<br><br>Maiores Informações:<br>(85) 98654.8192 - 11/04/2017\",\"countryUrl\":\"brasil\",\"neighborhoodUrl\":\"aldeota\",\"cityUrl\":\"fortaleza\",\"stateUrl\":\"ceara\",\"zoneUrl\":\"bairros\",\"accountName\":\"Imobiliária Remax Confiança\",\"accountLogo\":\"http://resizedimgs.vivareal.com/LpG4gB4pADpOF_qhDHdCSFpXTe0=/870x653/vr.images.sp/4265d339a7ac808c87bac8015b7a85e0.jpg\",\"accountRole\":\"INMOBILIARIA\",\"accountLicenseNumber\":\"13574-J-CE\",\"account\":\"97855\",\"email\":null,\"leadEmails\":null,\"contactName\":\"Imobiliária Remax Confiança\",\"contactLogo\":\"http://resizedimgs.vivareal.com/LpG4gB4pADpOF_qhDHdCSFpXTe0=/870x653/vr.images.sp/4265d339a7ac808c87bac8015b7a85e0.jpg\",\"contactPhoneNumber\":\"08530627797\",\"contactCellPhoneNumber\":\"085985327763\",\"contactAddress\":\"Rua Edília Rego Barros, 196 - Parquelândia\",\"usageId\":\"RESIDENCIAL\",\"usageName\":\"Residencial\",\"businessId\":\"VENTA\",\"businessName\":\"Venda\",\"publicationType\":\"STANDARD\",\"positioning\":0,\"salePrice\":1800000,\"baseSalePrice\":1800000,\"rentPrice\":null,\"baseRentPrice\":null,\"currency\":\"BRL\",\"numImages\":16,\"image\":\"http://resizedimgs.vivareal.com/IwrudYzoR31zPXqDqT7mqN4liRg=/fit-in/870x653/vr.images.sp/48c997b6baf8916708a295f65ce1f5f0.jpg\",\"thumbnail\":\"http://resizedimgs.vivareal.com/pMvH8vzNb9wXq1IQW48SloLKWU0=/fit-in/221x147/vr.images.sp/48c997b6baf8916708a295f65ce1f5f0.jpg\",\"showAddress\":true,\"address\":\"Rua Leonardo Mota, 1731\",\"zipCode\":\"60170041\",\"locationId\":\"BR>Ceara>NULL>Fortaleza>Barrios>Aldeota\",\"images\":[\"http://resizedimgs.vivareal.com/IwrudYzoR31zPXqDqT7mqN4liRg=/fit-in/870x653/vr.images.sp/48c997b6baf8916708a295f65ce1f5f0.jpg\",\"http://resizedimgs.vivareal.com/jPF0ncfB49kvGagSMTsrapbzS4g=/fit-in/870x653/vr.images.sp/5e840890e61045683b8d420a63586067.jpg\",\"http://resizedimgs.vivareal.com/gc77H3KlNjSuV3dJ4bfXIwhA_Yg=/fit-in/870x653/vr.images.sp/beae073b1e938375cbd9896b089db95f.jpg\",\"http://resizedimgs.vivareal.com/7sorPFW6yjar3ceC_HZaTRUu1E4=/fit-in/870x653/vr.images.sp/ceb0f906d1dd04bc1302b29cf614ac1a.jpg\",\"http://resizedimgs.vivareal.com/fXzIgBWZJhmzwkFa2EZME6lEOHQ=/fit-in/870x653/vr.images.sp/d9212a31f3efe3edd186f7678f030224.jpg\",\"http://resizedimgs.vivareal.com/HM7m0CVjaDdMT-aTguECnQeqKHk=/fit-in/870x653/vr.images.sp/88a894a5e68500be77d99e18893e94fb.jpg\",\"http://resizedimgs.vivareal.com/m2JeOQZKe4ncxhgyTI1M1m9eLsQ=/fit-in/870x653/vr.images.sp/8d1e9da7dbdecbd374b3fc140bea6d8d.jpg\",\"http://resizedimgs.vivareal.com/0j48-8OTZ57eXilvWPMPygYCRfs=/fit-in/870x653/vr.images.sp/46a221927b905826a5ff4ce9a472406b.jpg\",\"http://resizedimgs.vivareal.com/SlNZ5_zh3srgY1cquxFpwQ_i-bE=/fit-in/870x653/vr.images.sp/34d7e721d5f65b7fcd861534b8973f63.jpg\",\"http://resizedimgs.vivareal.com/GGZyy7FiyA-i1v3g4UNIfy5ArYE=/fit-in/870x653/vr.images.sp/aba6fefbde538194140b115ba7c7d26b.jpg\",\"http://resizedimgs.vivareal.com/2gfEQAC0AVc6YpJQomBrqzS77Tg=/fit-in/870x653/vr.images.sp/6500019f782d52f3e7d31b3b3c39374b.jpg\",\"http://resizedimgs.vivareal.com/L5AIXahZXlGXNKyCaP8hD-17fXs=/fit-in/870x653/vr.images.sp/a126f37182ce44d3ddf32b9b7182f91a.jpg\",\"http://resizedimgs.vivareal.com/9R4IVLxLeXsR6so91_mOcpii7Xo=/fit-in/870x653/vr.images.sp/cdc48dac1be03b1972561a30e7677cd1.jpg\",\"http://resizedimgs.vivareal.com/G5siquSk_RiJunIrOrur5B-r_R8=/fit-in/870x653/vr.images.sp/43f5be159fdbbabdb6ccc6dcf7955f12.jpg\",\"http://resizedimgs.vivareal.com/S906xC8cTmq7GVNlOue_9o2lYBs=/fit-in/870x653/vr.images.sp/ac5725f9df6a7a992d13e05fbb734c65.jpg\",\"http://resizedimgs.vivareal.com/mRxS5u8bnl1OgMpX7P_0M5vd-cw=/fit-in/870x653/vr.images.sp/f74616f924d67203ee7e4c1519c1be59.jpg\"],\"backgroundImage\":null,\"video\":null,\"constructionStatus\":null,\"rentPeriodId\":null,\"rentPeriod\":null,\"suites\":5,\"condominiumPrice\":1570,\"iptu\":null,\"additionalFeatures\":[\"Área de serviço\",\"Cozinha\",\"Ar condicionado\",\"Elevador\",\"Interfone\"],\"developmentInformation\":null,\"creationDate\":1479938893483,\"promotions\":[],\"geoDistance\":null,\"isFeatured\":false,\"streetId\":\"ceara/fortaleza/bairros/aldeota/rua-leonardo-mota\",\"streetName\":\"Rua Leonardo Mota\",\"streetNumber\":\"1731\",\"accountPagePath\":\"/97855/imobiliaria-remax-confianca/\",\"links\":[{\"rel\":\"self\",\"href\":\"http://api.vivareal.com/api/1.0/listings/property/75055432\"},{\"rel\":\"contact\",\"href\":\"http://api.vivareal.com/api/1.0/listings/property/75055432/contact\"},{\"rel\":\"similar\",\"href\":\"http://api.vivareal.com/api/1.0/listings/property/75055432/similar\"}]}";
}

class SearchResponseList implements Iterable<SearchApiResponse> {

    private List<SearchApiResponse> responses;

    SearchResponseList(List<SearchApiResponse> responses) {
        this.responses = responses;
    }

    @Override
    public Iterator<SearchApiResponse> iterator() {

        Random rnd = new Random();
        return new Iterator<SearchApiResponse>() {
            private Iterator<SearchApiResponse> original = responses.iterator();

            @Override
            public boolean hasNext() {
                return original.hasNext();
            }

            @Override
            public SearchApiResponse next() {
                try {
                    Thread.sleep(rnd.nextInt(2000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return original.next();
            }
        };
    }
}
