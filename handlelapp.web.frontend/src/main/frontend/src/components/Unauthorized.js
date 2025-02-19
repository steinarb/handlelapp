import React from 'react';
import { useSelector } from 'react-redux';
import {
    useGetDefaultlocaleQuery,
    useGetLoginstateQuery,
    useGetDisplaytextsQuery,
    useGetLogoutMutation,
} from '../api';
import Container from './bootstrap/Container';
import ChevronLeft from './bootstrap/ChevronLeft';


export default function Unauthorized() {
    const { isSuccess: defaultLocaleIsSuccess } = useGetDefaultlocaleQuery();
    const locale = useSelector(state => state.locale);
    const { data: loginresult = { user: {} } } = useGetLoginstateQuery(locale, { skip: !defaultLocaleIsSuccess });
    const { data: text = [] } = useGetDisplaytextsQuery(locale, { skip: !defaultLocaleIsSuccess });
    const username = loginresult.user.username;
    const [ getLogout ] = useGetLogoutMutation();
    const onLogoutClicked = async () => { await getLogout() }

    return (
        <div>
            <nav className="navbar navbar-light bg-light">
                <a className="btn btn-primary left-align-cell" href="../..">
                    <ChevronLeft />&nbsp;{text.gohome}!
                </a>
                <h1>{text.noaccess}</h1>
                <div className="col-sm-2"></div>
            </nav>
            <Container>
                <p>{text.hi} {username}! {text.noaccessmessage1}</p>
                <p>{text.noaccessmessage2}</p>
                <form onSubmit={ e => { e.preventDefault(); }}>
                    <div className="form-group row">
                        <div className="col-5"/>
                        <div className="col-7">
                            <button className="btn btn-primary" onClick={onLogoutClicked}>{text.logout}</button>
                        </div>
                    </div>
                </form>
            </Container>
        </div>
    );
}
